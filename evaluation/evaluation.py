import os
import sys
import numpy as np
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import RandomizedSearchCV
from initialize import load_tmp_file, ALL_SEGMENTS, \
    REAL_SEGMENTS, GROUNDTRUTH_DIR


SLOT_3_LEAVING = [
    [(2, 0, 0), (2, 1, 1), (2, 0, 1)],
    [(2, 0, 0), (0, 0, 0), (2, 1, 0)]
]

RESULT_TRUE_POSITIVE = 1
RESULT_TRUE_NEGATIVE = 2
RESULT_FALSE_POSITIVE = -1
RESULT_FALSE_NEGATIVE = -2
RESULT_EARLY_FALSE_POSITIVE = -3
WINDOW_SIZE = 5
SLOT_SIZE = 4

seed = 21
ESTIMATOR = 20
MAX_DEPTH = 6
MIN_SAMPLE_LEAF = 25

random_grid = {
    'n_estimators': [20],
    'max_depth': [5, 10, 15, 20, 25, 30, 35, 40, 45, 50],
    'min_samples_leaf': [2, 4, 8, 16, 32, 64],
    'min_samples_split': [2, 4, 8, 16, 32]
}

# 20190430 Machine tuning
A_ESTIMATOR = 20
A_DEPTH = 30
A_LEAF = 8
A_SPLIT = 2
B_ESTIMATOR = 20
B_DEPTH = 40
B_LEAF = 32
B_SPLIT = 2
C_ESTIMATOR = 20
C_DEPTH = 25
C_LEAF = 2
C_SPLIT = 4

RESULT_CATE = ['precision1', 'recall1', 'precision2', 'recall2',
               'false1', 'false2', 'delay1', 'delay2']


LOCATIONS = ['library', 'restaurant', 'busstop', 'car', 'coffee',
             'venue', 'lounge', 'office']

EXPERIMENTERS = ['1', '2', '3', '4', '5', '6', '7', '8']


SEGMENTS_DICT = {segment['name']: segment
                 for segment in ALL_SEGMENTS + REAL_SEGMENTS}


def load_ground_truth(name, directory=GROUNDTRUTH_DIR, delimiter=' '):
    with open(os.path.join(directory, name + '.gt'), 'r') as fp:
        gt = []
        for line in fp:
            elem = line.strip().split(delimiter)
            gt.append([int(elem[1]), int(elem[2]), int(elem[3])])
        return gt


def concat_dataset(names, without_ground=False):
    xs = []
    ys = []
    devi = []
    for name in names:
        tmp = load_tmp_file(name)
        if not without_ground:
            ground = load_ground_truth(name)
            ys += ground
        xs += tmp['feature']
        devi.append(tmp['deviation'])
    return xs, ys, devi


def decision_maker(ys, dismiss=5):
    final1 = []
    final2 = []
    for i in range(len(ys)):
        if i < 5:
            continue
        else:
            f1 = hard_coded_leaving_pattern(ys[i - 3: i + 1])
            f2 = hard_coded_left_decision(ys[i - 3: i + 1])
            if f1:
                if not final1 or final1[-1][0] + final1[-1][1] < i - dismiss:
                    final1.append([i, 1])
                else:
                    final1[-1][1] = i - final1[-1][0] + 1
            if f2:
                if not final2 or final2[-1][0] + final2[-1][1] < i - dismiss:
                    final2.append([i, 1])
                else:
                    final2[-1][1] = i - final2[-1][0] + 1
    return final1, final2


def hard_coded_left_decision(sel_ys):
    c1, c2, c3 = zip(*sel_ys)
    if sum(c3) <= len(sel_ys)*0.4 and c3[-1] == 0 \
            and sum(c2) <= len(sel_ys)*0.4 and c2[-1] == 0:
        return True
    else:
        return False


def hard_coded_leaving_pattern(sel_ys):
    cur = sel_ys[-1]
    if cur not in SLOT_3_LEAVING[1]:
        return False
    else:
        score = 0
        for i in range(-2, -len(sel_ys) - 1, -1):
            slt = sel_ys[i]
            if slt in SLOT_3_LEAVING[0]:
                score += 1
        if score >= len(sel_ys) - 2:
            return True
        else:
            return False


"""
Structure of the decisions

([], []) empty - no alert
([3, 4], [2, 3]) 

"""


class AlertEvaluator:

    @property
    def d1_positive(self):
        return len([1 for grt in self.groundtruth if grt['point1'] != -1])

    @property
    def d2_positive(self):
        return len([1 for grt in self.groundtruth if grt['point2'] != -1])

    @property
    def d1_negative(self):
        return len(self.groundtruth) - self.d1_positive

    @property
    def d2_negative(self):
        return len(self.groundtruth) - self.d2_positive

    def status(self, d='status1'):
        tp = 0
        tn = 0
        fp = 0
        fn = 0
        for record in self.general:
            stat = record[d]
            if stat == RESULT_TRUE_POSITIVE:
                tp += 1
            elif stat == RESULT_TRUE_NEGATIVE:
                tn += 1
            elif stat == RESULT_FALSE_NEGATIVE:
                fn += 1
            elif stat == RESULT_FALSE_POSITIVE:
                fp += 1
            elif stat == RESULT_EARLY_FALSE_POSITIVE:
                fp += 1
                fn += 1

        return tp, tn, fp, fn

    def get_precision_recall(self, d='status1'):
        tp, tn, fp, fn = self.status(d)
        if tp + fp == 0:
            res1 = 1.0
        else:
            res1 = tp / (tp + fp)

        if tp + fn == 0:
            res2 = 1.0
        else:
            res2 = tp / self.d2_positive
        return res1, res2

    def get_true_positive_rate(self, d='status1'):
        tp, tn, fp, fn = self.status(d)
        if tp + fn == 0:
            res = 1.0
        else:
            res = tp / (tp + fn)
        return res

    def get_false_positive_rate(self, d='status2'):
        tp, tn, fp, fn = self.status(d)
        if fp + tn == 0:
            res = 0.0
        else:
            res = fp / (fp + tn)
        return res

    def get_false_alarm(self, d='status1'):
        tp, tn, fp, fn = self.status(d)
        if fp + tn == 0:
            return 0.0
        return fp / (fp + tn)

    def get_delay(self, d='status1', v='value1'):
        delays = []
        for record in self.general:
            stat = record[d]
            if stat == RESULT_TRUE_POSITIVE:
                delays.append(record[v])
        return delays

    def general_analysis(self):
        summary = []
        for res, grt, devi in zip(self.decisions, self.groundtruth,
                                  self.devis):
            name = grt['name']
            # adjust deviation
            p1 = -1 if grt['point1'] == -1 else grt['point1'] - devi
            p2 = -1 if grt['point2'] == -1 else grt['point2'] - devi

            al1, par1 = self.__find_nearest_alert(res['c1'], res['l1'],
                                                  p2, p1)
            al2, par2 = self.__find_nearest_alert(res['c2'], res['l2'],
                                                  p2, p1)
            result = {
                'name': name,
                'status1': al1,
                'value1': par1,
                'status2': al2,
                'value2': par2,
            }
            if al1 < 0 :
                if al1 == RESULT_FALSE_POSITIVE:
                    print(name, "FP")
                elif al1 == RESULT_FALSE_NEGATIVE:
                    print(name, "FN")
                elif al1 == RESULT_EARLY_FALSE_POSITIVE:
                    print(name, "early FP")
            summary.append(result)
        return summary

    @staticmethod
    def __find_nearest_alert(obs_c, obs_l, grt, p1):
        if not obs_c:
            if grt == -1:
                return RESULT_TRUE_NEGATIVE, 0
            else:
                return RESULT_FALSE_NEGATIVE, 0
        elif grt == -1:
            return RESULT_FALSE_POSITIVE, len(list(obs_c))
        else:
            result = 0
            flag = False
            for poi, last in zip(obs_c, obs_l):
                delta = (poi + 1) * WINDOW_SIZE - grt
                if delta + grt < p1 or delta < -15:
                    return RESULT_EARLY_FALSE_POSITIVE, 0

                if (poi + last + 1) * WINDOW_SIZE >= p1 and delta < 60:
                    result = delta
                    flag = True
                    break
            if flag:
                return RESULT_TRUE_POSITIVE, result
            else:
                return RESULT_FALSE_NEGATIVE, 0

    def __init__(self, decisions, groundtruth, devis):
        # decisions should be mapped to the groundtruth
        # groundtruth should follow the structure

        tmp_decision = []
        for dec1, dec2 in decisions:
            if not dec1:
                point1 = []
                last1 = []
            else:
                point1, last1 = zip(*dec1)
            if not dec2:
                point2 = []
                last2 = []
            else:
                point2, last2 = zip(*dec2)
            tmp_decision.append(
                {'c1': point1,
                 'c2': point2,
                 'l1': last1,
                 'l2': last2}
            )
        self.decisions = tmp_decision
        self.devis = devis
        self.groundtruth = groundtruth
        self.general = self.general_analysis()


def join_subs(sub_li, t):
    result = []
    for i, sub in enumerate(sub_li):
        if i == t:
            continue
        result += sub.tolist()
    return result


def cross_validation(name_groups, k):
    sub_groups = []
    group_results = []
    for group in name_groups:
        tmp_names = np.array(group)
        np.random.shuffle(tmp_names)
        sub = np.array_split(tmp_names, k)
        sub_groups.append(sub)
        group_results.append({'precision1': [],
                              'recall1': [],
                              'precision2': [],
                              'recall2': [],
                              'false1': [],
                              'false2': [],
                              'delay1': [],
                              'delay2': [],
                              'category': tuple(tmp_names[0].split('_')[4: 6])})

    for t in range(k):
        all_train = []
        sep_test = []
        for sub in sub_groups:
            test = sub[t].tolist()
            train = join_subs(sub, t)
            all_train += train
            sep_test.append(test)

        for i, test in enumerate(sep_test):
            result = evaluate_once(all_train, test)
            group_results[i]['precision1'].append(result['status1_pr'][0])
            group_results[i]['recall1'].append(result['status1_pr'][1])
            group_results[i]['precision2'].append(result['status2_pr'][0])
            group_results[i]['recall2'].append(result['status2_pr'][1])
            group_results[i]['false1'].append(result['status1_fa'])
            group_results[i]['false2'].append(result['status2_fa'])
            group_results[i]['delay1'] += result['delay1']
            group_results[i]['delay2'] += result['delay2']

    return group_results


def parameter_tuning(x, y, scoring):
    rf = RandomForestClassifier()
    rf_random = RandomizedSearchCV(estimator=rf,
                                   param_distributions=random_grid,
                                   n_iter=100,
                                   cv=10,
                                   n_jobs=-1,
                                   scoring=scoring,
                                   random_state=seed
                                   )
    rf_random.fit(x, y)
    print(rf_random.best_params_)


def subclassifier_parameter_tuning(all_sets):
    x, y, devi = concat_dataset(all_sets)
    x = np.array(x)
    y1, y2, y3 = zip(*y)
    print('=====Sub classifier 1=====')
    parameter_tuning(x, y1, 'f1_micro')
    print('=====Sub classifier 2=====')
    parameter_tuning(x, y2, 'roc_auc')
    print('=====Sub classifier 3=====')
    parameter_tuning(x, y3, 'roc_auc')


def evaluate_once(trains, test):
    print("model training")
    xa, ya, devi = concat_dataset(trains)

    dtc1 = RandomForestClassifier(n_estimators=A_ESTIMATOR,
                                  min_samples_leaf=A_LEAF,
                                  max_depth=A_DEPTH,
                                  min_samples_split=A_SPLIT,
                                  random_state=seed)
    dtc2 = RandomForestClassifier(n_estimators=B_ESTIMATOR,
                                  min_samples_leaf=B_LEAF,
                                  max_depth=B_DEPTH,
                                  min_samples_split=B_SPLIT,
                                  random_state=seed)
    dtc3 = RandomForestClassifier(n_estimators=C_ESTIMATOR,
                                  min_samples_leaf=C_LEAF,
                                  max_depth=C_DEPTH,
                                  min_samples_split=C_SPLIT,
                                  random_state=seed)

    ya1, ya2, ya3 = zip(*ya)
    dtc1.fit(xa, ya1)
    dtc2.fit(xa, ya2)
    dtc3.fit(xa, ya3)
    print("classifying")
    decisions = []
    devis = []
    for i, nam in enumerate(test):
        xb, yb, devi = concat_dataset([nam], without_ground=True)

        ybp = list(zip(dtc1.predict(xb), dtc2.predict(xb), dtc3.predict(xb)))

        devis += devi
        res1, res2 = decision_maker(ybp)
        decisions.append([res1, res2])
    print("List of false detection (record name, type of error): ")
    reference = [SEGMENTS_DICT[tes] for tes in test]
    evaluator = AlertEvaluator(decisions, reference, devis)
    st1p, st1r = evaluator.get_precision_recall('status1')
    st2p, st2r = evaluator.get_precision_recall('status2')
    fa1 = evaluator.get_false_alarm('status1')
    fa2 = evaluator.get_false_alarm('status2')
    dl1 = evaluator.get_delay('status1', 'value1')
    dl2 = evaluator.get_delay('status2', 'value2')
    return {
        'status1_pr': (st1p, st1r),
        'status2_pr': (st2p, st2r),
        'status1_fa': fa1,
        'status2_fa': fa2,
        'delay1': dl1,
        'delay2': dl2,
        'all4': evaluator.status('status2')
    }


def load_results(filename):
    with open(filename, 'r') as fp:
        for line in fp:
            name, elem = line.strip().split(':')
            nums = [float(num) for num in elem.split(',')]
            if name[-1] == '2': # only display required
                continue
            if name not in ['delay1', 'delay2']:
                print('%s: %f (%f)' % (name[:-1],
                                       np.average(nums),
                                       np.std(nums)))
            else:
                print('%s: %f (%f) secs' % (name[:-1],
                      np.average(np.array(nums) * 0.05),
                      np.std(np.array(nums) * 0.05)))


def ideal_single_combo(t=10):
    a = ['0', '45', '90']
    b = ['normal', 'slow', 'fast']
    name_group = {}
    for angle in a:
        for speed in b:
            name_group[(angle, speed)] = []

    for item in ALL_SEGMENTS[:287]:
        ag, sp = item['name'].split('_')[4: 6]
        name_group[(ag, sp)].append(item['name'])

    final_group = []
    final_results = []
    for angle in a:
        for speed in b:
            final_group.append(name_group[(angle, speed)])
            final_results.append({name: [] for name in RESULT_CATE})

    for i in range(t):
        results = cross_validation(final_group, 4)
        for j, result in enumerate(results):
            for cate in RESULT_CATE:
                final_results[j][cate] +=  result[cate]

    cnt = 0
    for angle in a:
        for speed in b:
            with open('result_%s_%s.txt' % (angle, speed), 'w') as fp:
                for cate in RESULT_CATE:
                    to_write = final_results[cnt][cate]
                    fp.write(cate + ':'
                             + ','.join([str(i) for i in to_write]) + '\n')
            cnt += 1


def real_experiment_overall():
    results = evaluate_once([segment['name'] for segment in ALL_SEGMENTS[:287]],
                            [segment['name'] for segment in REAL_SEGMENTS])
    precision1, recall1 = results['status1_pr']
    delay1 = results['delay1']

    print("precision", precision1)
    print("recall", recall1)
    print("ave. delay: ", np.average(np.array(delay1) * 0.05))



def get_dtcs():
    trains = [segment['name'] for segment in ALL_SEGMENTS[:287]]
    xa, ya, devi = concat_dataset(trains)

    dtc1 = RandomForestClassifier(n_estimators=A_ESTIMATOR,
                                  min_samples_leaf=A_LEAF,
                                  max_depth=A_DEPTH,
                                  min_samples_split=A_SPLIT,
                                  random_state=seed)
    dtc2 = RandomForestClassifier(n_estimators=B_ESTIMATOR,
                                  min_samples_leaf=B_LEAF,
                                  max_depth=B_DEPTH,
                                  min_samples_split=B_SPLIT,
                                  random_state=seed)
    dtc3 = RandomForestClassifier(n_estimators=C_ESTIMATOR,
                                  min_samples_leaf=C_LEAF,
                                  max_depth=C_DEPTH,
                                  min_samples_split=C_SPLIT,
                                  random_state=seed)
    ya1, ya2, ya3 = zip(*ya)
    dtc1.fit(xa, ya1)
    dtc2.fit(xa, ya2)
    dtc3.fit(xa, ya3)
    return dtc1, dtc2, dtc3


def real_experiment_category(places):
    selected = [segment['name'] for segment in REAL_SEGMENTS
                    if segment['name'].split('_')[1] == places]

    pos = 0
    neg = 0
    for segment in REAL_SEGMENTS:
        if segment['name'] in selected:
            if segment['point1'] != -1:
                pos += 1
            else:
                neg += 1
    print("# of positive cases: ", pos)
    print("# of negative cases: ", neg)
    results = evaluate_once([segment['name'] for segment in ALL_SEGMENTS[:287]],
                            selected)

    precision1, recall1 = results['status1_pr']
    delay1 = results['delay1']
    print("precision", precision1)
    print("recall", recall1)
    print("ave. delay: ", np.average(np.array(delay1) * 0.05))


def real_experiment_user(users):
    selected = [segment['name'] for segment in REAL_SEGMENTS
                if segment['name'].split('_')[3] == users]
    pos = 0
    neg = 0
    for segment in REAL_SEGMENTS:
        if segment['name'] in selected:
            if segment['point1'] != -1:
                pos += 1
            else:
                neg += 1
    print("# of positive cases: ", pos)
    print("# of negative cases: ", neg)
    for location in LOCATIONS:
        num = 0
        for i in selected:
            if i.split('_')[1] == location:
               num += 1
        print(location, num)

    results = evaluate_once([segment['name'] for segment in ALL_SEGMENTS[:287]],
                            selected)

    precision1, recall1 = results['status1_pr']
    delay1 = results['delay1']
    print("precision", precision1)
    print("recall", recall1)
    print("ave. delay: ", np.average(np.array(delay1) * 0.05))


def ideal_2p_experiments():
    train = [segment['name'] for segment in ALL_SEGMENTS[:287]]
    dist = ['30', '75', '100']
    for dis in dist:
        choice = [segment['name'] for segment in ALL_SEGMENTS[287:404]
                  if dis in segment['name'].split('_')[-6]]
        results = evaluate_once(train, choice)
        precision1, recall1 = results['status1_pr']
        delay1 = results['delay1']
        print('============== Distance: %s cm ==============' % dis)
        print("precision", precision1)
        print("recall", recall1)
        print("ave. delay: ", np.average(np.array(delay1) * 0.05))


def lab_experiment_speed_orientation():
    print("Generating results...")
    print("NOTE: the training process may take several minutes. Please wait.")
    ideal_single_combo(10)
    a = ['0', '45', '90']
    b = ['slow', 'normal', 'fast']
    print("Lab experiment: departing speed and orientation angles")
    for angle in a:
        for speed in b:
            print('============== Angle: %s, Speed: %s =============='
                  % (angle, speed))
            load_results('result_%s_%s.txt' % (angle, speed))


def lab_experiment_nearby_stranger():
    print("Lab experiment: nearby stranger")
    ideal_2p_experiments()


def real_world_per_location():
    print("Real-world experiments: per-location results")
    for place in LOCATIONS:
        print('============== Location: %s ==============' % place)
        real_experiment_category(place)


def real_world_per_user():
    print("Real-world experiments: per-user results")
    for user in EXPERIMENTERS:
        print('============== User: %s ==============' % user)
        real_experiment_user(user)


if __name__ == '__main__':
    experiments = [
        'lab_speed_angle',
        'lab_nearby',
        'real_world_overall',
        'real_world_per_user',
        'real_world_per_location'
    ]

    if len(sys.argv) > 1:
        experiment = sys.argv[1]
        if experiment == experiments[0]:
            lab_experiment_speed_orientation()
        elif experiment == experiments[1]:
            lab_experiment_nearby_stranger()
        elif experiment == experiments[2]:
            real_experiment_overall()
        elif experiments == experiments[3]:
            real_world_per_user()
        elif experiments == experiments[4]:
            real_world_per_location()
        else:
            print("This script supports the following experiments: ")
            for item in experiments:
                print(item)
    else:
        print("This script supports the following experiments: ")
        for item in experiments:
            print(item)
        print("Please run \"python evaluation.py <EXPERIMENT_CODE>\" "
              "and replace <EXPERIMENT_CODE> with the above code.")
