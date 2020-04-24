from ASAnalyzer.helper import *
from ASAnalyzer.constant import *
from ASAnalyzer.kalman1d import Kalman1DAcc
from ASAnalyzer.logger import logger


class ASAnalyzer:
    # Dynamic Acoustic Signal Analyzer
    # Data is fed one time sample by another

    @property
    def time_unit(self):
        # get the time unit in second
        return self.period / self.sampling_rate

    @property
    def dist_unit(self):
        # get the distance unit per time unit
        return self.down_sample / self.sampling_rate * self.sound_speed / 2

    def is_noise(self, col):
        return np.median(col[self.far_index:150]) > self.far_noise

    def is_diff_noise(self, col):
        return np.average(col[self.far_index:150]) > self.far_diff_noise

    def glitch_extractor(self, delt):
        size = 0
        for i in range(delt.shape[0]):
            if delt[i] > self.glitch_noise:
                size += 1
        return size

    def add_data(self, col, subt=abs_subt):
        # Note: the first column is not used for computation
        self.raw.append(col)

        if not self.first_flag:
            if not self.is_noise(col):
                self.first_flag = True
            else:
                self.first_offset += 1
            return -1
        delt = subt(col, self.raw[-2])

        if (len(self.raw) - self.first_offset) % 5 == 0:
            stat = self.get_static_candidates(np.array(self.raw[-5:]))
            self.static_obj.append(stat)

        # whether the first slot is noisy:

        if self.is_noise(col) or self.is_diff_noise(delt):
            # is noisy:
            self.delta.append(np.zeros_like(delt))
            self.noise_ctr += 1
            logger.info('noise')
        else:
            self.delta.append(delt)
            self.noise_ctr = 0

        # self.delta.append(subt(col, self.raw[-2]))
        candidates = self.get_point_candidates(self.delta[-1])
        self.candidates.append(candidates)
        pos = self.position_estimate(candidates)
        logger.info('[WARN] at {:.2f} ({:d}) s, the user is {:.2f} m away'
                    .format(len(self.delta) * self.time_unit,
                            len(self.delta),
                            pos * self.dist_unit))
        self.user_trace.append([len(self.delta), pos])

    def position_estimate(self, candidates):
        def is_valid_movement(cur_vd, mv_st, mv_ed, est_pos, max_range, min_range):
            if cur_vd == 0:
                return abs(mv_st - est_pos) < max_range or abs(mv_ed - est_pos) < max_range
            elif cur_vd < 0:
                return mv_ed > est_pos - max_range and mv_st < est_pos + min_range
            else:
                return mv_ed > est_pos - min_range and mv_st < est_pos + max_range



        est_xp = self.prev_d if self.prev_d > 0 else self.dk_x0
        est_vp = 0 if len(self.est_v) < 5 else self.est_v[-1]
        obs = -1
        max_power = -1
        pow_eff = 0
        cur_v = 0
        if len(self.est_v) > 5:
            cur_v = int(self.est_v[-1] > 2) - int(self.est_v[-1] < -2)
            for i in range(len(self.est_v) - 2, 0, -1):
                vn = int(self.est_v[i] > 2) - int(self.est_v[i] < -2)
                if vn == cur_v and cur_v != 0:
                    pow_eff += 1
                else: 
                    break
        for st, size, peak in candidates:
            mx_mv = max(self.max_movement * np.power(self.discount, pow_eff),
                        self.min_movement)


            if is_valid_movement(cur_v, st, st + size, est_xp + est_vp, self.max_movement, mx_mv):
                magn = np.max(self.delta[-1][st: st + size])
                if magn > max_power * 1.15:
                    obs = peak
                    max_power = magn

        if obs != -1:

            add_thing = np.array([obs])
            self.no_obs_turns = 0
            # logger.info('Go with observation')
        elif self.prev_d == -1:
            add_thing = np.array([self.dk_x0])
            self.no_obs_turns += 1
            # logger.info('Go with start')
            # new_pos = self.dkal.add(np.array([0.]))
        else:
            # logger.info('Go with previous d')
            add_thing = np.array([self.est_d[-1]])
            self.no_obs_turns += 1
            if self.no_obs_turns > 2:
                add_thing = np.array([self.est_d[-2]])
            # if self.prev_d == -1:
            # new_pos = self.dkal.add(np.array([est_xp]))
            # else:
            #     new_pos = self.dkal.add(np.array([est_xp]))
            # new_pos = self.dkal.add(est_xp)
        new_pos = self.dkal.add(add_thing)
        d = new_pos[0, 0]
        v = new_pos[1, 0]
        # detect if the estimated position is out of range
        if d < 5:
            d = 5
            v = 0

#         if(len(self.obs_d) > 1000):
#             print("that's fine")

        if obs == -1:
            self.observed.append(False)
        else:
            self.observed.append(True)

        self.prev_d = d
        self.est_d.append(d)
        self.est_v.append(v)
        self.obs_d.append(add_thing[0])
        return d

    def __step_compare(self, x, value):
        steps = self.min_theta
        for met, val in steps:
            if x <= self.get_distance_index(met):
                return value >= val
        return False

    def outlier_filter(self, point_index, col):
        return [point for point in point_index
                if self.__step_compare(point, col[point])]

    def get_point_candidates(self, col, choose_peak=True):
        # get all candidate points

        # exclude extreme near
        extreme_near = self.extreme_near

        sel_col = np.copy(col)
        # glitch_size = self.glitch_extractor(sel_col)
        # if glitch_size > self.glitch_size:
        #     sel_col = np.zeros_like(sel_col)
        sel_col[:extreme_near] = 0
        start_index = self.start_index
        a = sel_col[extreme_near:start_index + 1]
        a[a < self.direct_high_pass] = 0
        sel_col[extreme_near:start_index + 1] = a

        point_index = median_outlier_index(sel_col, m=self.outlier_mul)
        # point_index = percentile_outlier_index(sel_col, p=self.outlier_p)
        # candidates [start index, size, peak index]
        candidates = []
        last_one = -1
        point_index = self.outlier_filter(point_index, sel_col)
        for i in point_index:
            if len(candidates) == 0:
                candidates.append([i, 1, i])
                last_one = i
            else:
                if i - last_one > self.cluster_tolerance:
                    candidates.append([i, 1, i])
                else:
                    candidates[-1][1] += i - last_one
                    if choose_peak:
                        cur_peak = candidates[-1][2]
                        candidates[-1][2] = cur_peak \
                            if sel_col[cur_peak] > sel_col[i] else i
                        # candidates[-1][2] = candidates[-1][0] + int(np.argmax(
                        #     col[candidates[-1][0]: i + 1]))
                last_one = i


        return candidates

    def get_static_candidates(self, raw_cols):
        prev = 0
        cols = raw_cols.copy()
        for dist, thres in self.raw_theta:
            dist_ind = self.get_distance_index(dist)
            sel = cols[:, prev: dist_ind]
            sel[sel < thres] = 0
            sel[sel >= thres] = 1
            cols[:, prev: dist_ind] = sel
            prev = dist_ind
        cols[:, prev:] = 0
        # Find the indices of 1's in each column
        static_obj = []
        for co in range(cols.shape[0]):
            ind = np.argwhere(cols[co, :] == 1).flatten().tolist()
            box = []
            for i in ind:
                if not box:
                    box = [[i, 1]]
                    continue
                if i <= box[-1][0] + box[-1][1]:
                    box[-1][1] += 1
                else:
                    box.append([i, 1])
            clean_box = [[i, n] for i, n in box if n > 1]
            # print(len(self.raw), clean_box)
            if not static_obj:
                static_obj = clean_box
            else:
                # intersection operation
                i = 0
                j = 0
                new_static = []
                while i < len(static_obj) and j < len(clean_box):
                    st_a, l_a = static_obj[i]
                    st_b, l_b = clean_box[j]
                    if st_a > st_b + l_b:
                        j = j + 1
                        continue
                    if st_b > st_a + l_a:
                        i = i + 1
                        continue
                    st_n = max(st_a, st_b)
                    ed_n = min(st_a + l_a, st_b + l_b)
                    l_n = ed_n - st_n
                    new_static.append([st_n, l_n])
                    i = i + 1
                    j = j + 1
                static_obj = new_static
        return static_obj

    def get_raw_cv(self, st, ed, size=5):
        size = size if size < len(self.raw) else len(self.raw)
        dx = np.array(self.raw)
        sel = dx[-size:, st:ed]
        print(sel.shape)
        print(np.std(sel, axis=0)/np.mean(sel, axis=0))

    def extract_window_features(self, window_size=5):
        raw_win = self.raw[-window_size:]
        raw_delta = self.delta[-window_size:]
        tiles = self.__make_tiles(window_size, [10, 50])
        raw_tiles = self.__make_tiles_raw(window_size, [10, 50])
        return [
            self.win_est_position(window_size),
            self.win_obs_position(window_size),
            self.win_est_position_dev(window_size),
            self.win_obs_position_dev(window_size),
            self.win_lost(window_size),
            self.win_est_speed(window_size),
            self.win_obs_speed(window_size),
            self.win_est_mean_speed(window_size),
            self.win_est_fluctuation(window_size),
            self.win_tile_magnitude(tiles)[0],
            self.win_tile_magnitude(tiles)[1],
            self.win_tile_hot_ratio(tiles, 200),
        ] + self.win_tile_variance(raw_tiles).tolist() + list(self.static_obj_diff())


    def hard_coded_left_decision(self, slots=5):
        c1, c2, c3 = zip(*self.cl_results[-slots:])
        if sum(c3) <= slots*0.2 and c3[-1] == 0:
            return True
        else:
            return False

    def hard_coded_leaving_pattern(self, slots=5):
        cur = self.cl_results[-1]
        if cur not in SLOT_3_LEAVING[0]:
            return False
        else:
            score = 0
            for i in range(-2, -slots, -1):
                slt = self.cl_results[i]
                if slt in SLOT_3_LEAVING[1]:
                    score += 1
            if score > slots - 2:
                return True
            else:
                return False

    # ===============
    # Feature Extract
    # ===============
    def __make_tiles(self, window_size, detect_range):
        st, ed = detect_range
        sel = np.array([item[st: ed] for item in self.delta[-window_size:]])
        return sel

    def __make_tiles_raw(self, window_size, detect_range):
        st, ed = detect_range
        sel = np.array([item[st: ed] for item in self.raw[-window_size:]])
        return sel

    def win_est_position(self, window_size=5):
        return np.average(self.est_d[-window_size:])

    def win_est_position_dev(self, window_size=5):
        return (self.win_est_position(window_size) - np.median(self.est_d))/np.median(self.est_d)

    def win_est_mean_speed(self, window_size=5):
        return np.average(self.est_v[-window_size:])

    def win_est_speed(self, window_size=5):
        return (self.est_d[-1] - self.est_d[-window_size]) / window_size

    def win_obs_position(self, window_size=5):
        return np.average(self.obs_d[-window_size:])

    def win_obs_position_dev(self, window_size=5):
        return (self.win_obs_position(window_size) - np.median(self.obs_d))/np.median(self.obs_d)

    def win_obs_speed(self, window_size=5):
        return (self.obs_d[-1] - self.obs_d[-window_size]) / window_size

    def win_curvature(self):
        return 0

    def win_est_fluctuation(self, window_size):
        direction = (self.est_v[-window_size] >= 0)
        counter = 0
        for v in self.est_v[-window_size + 1:]:
            cur = (v >= 0)
            if cur != direction:
                counter += 1
            direction = cur
        return counter

    def win_tile_magnitude(self, sel):
        return np.average(sel), np.median(sel)

    def win_tile_hot_ratio(self, sel, threshold):
        return np.where(sel >= threshold)[0].shape[0] \
               / sel.shape[0] / sel.shape[1]

    def win_tile_variance(self, sel):
        return np.std(sel, axis=0)

    def static_obj_diff(self, win_index=-1):
        cp1 = self.static_obj[win_index]
        cp2 = self.static_obj[0]
        i = 0
        j = 0
        more_obj = 0
        less_obj = 0
        while(i < len(cp1) and j < len(cp2)):
            st_1, ln_1 = cp1[i]
            st_2, ln_2 = cp2[j]
            # any >1 overlap will be regarded as the same one
            if st_1 + ln_1 < st_2:
                more_obj += 1
                i = i + 1
            elif st_2 + ln_2 < st_1:
                less_obj += 1
                j = j + 1
            else:
                i = i + 1
                j = j + 1
        if i < len(cp1):
            more_obj += len(cp1) - i
        if j < len(cp2):
            less_obj += len(cp2) - j
        return more_obj, less_obj

    def win_lost(self, window_size):
        for is_obs in self.observed[-window_size:]:
            if is_obs:
                return True
        return False

    def __distances(self):
        if len(self.raw) == 0:
            return None
        else:
            return [i * self.dist_unit for i in range(self.raw[0].shape[0])]

    def get_distance_index(self, meter):
        return int(meter / self.dist_unit)

    def __init__(self, settings=DEFAULT_SETTINGS):
        for key, value in settings.items():
            setattr(self, key, value)
        # raw heatmap data and delta heatmap data
        self.raw = []
        self.delta = []
        self.observed = []
        self.first_flag = False
        self.noise_ctr = 0
        self.first_offset = 0

        self.__tmp_slot = []
        self.__tmp_window = []
        self.user_trace = []
        self.candidates = []
        self.prev_d = -1
        self.no_obs_turns = 0
        # self.prev_v = 0

        self.static_obj = []

        self.est_d = []
        self.est_v = []
        self.obs_d = []

        self.dkal = Kalman1DAcc(t=1, R=self.dk_R, Q=self.dk_Q,
                                initX0=self.dk_x0)
        # self.dkal = Kalman1D(R=self.dk_R, initX=self.dk_x0)

        # Final Decision
        self.cl_results = []
