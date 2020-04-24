import os
import sys
import json
from configparser import ConfigParser
from ASAnalyzer import ASAnalyzer
from ASAnalyzer.utils import load_menu, load_segment
from ASAnalyzer.constant import SETTINGS_PRESET


cfg = ConfigParser()
cfg.read('settings.ini')
SEGMENT_DIR = cfg.get('directory', 'lab_dir')
MENU_DIR = cfg.get('directory', 'lab_menu_dir')
MENU_FILE = os.path.join(MENU_DIR, cfg.get('directory', 'menu'))

REAL_SEGMENT_DIR = cfg.get('directory', 'real_dir')
REAL_MENU_DIR = cfg.get('directory', 'real_menu_dir')
REAL_MENU_FILE = os.path.join(REAL_MENU_DIR, cfg.get('directory', 'menu'))

IDEAL_SEGMENTS = load_menu(MENU_FILE, SEGMENT_DIR)
REAL_SEGMENTS = load_menu(REAL_MENU_FILE, REAL_SEGMENT_DIR)
ALL_SEGMENTS = IDEAL_SEGMENTS + REAL_SEGMENTS
SEGMENTS_MAX_WINDOWS = {item['name']: int((item['end'] - item['start']) / 5)
                        for item in ALL_SEGMENTS}

GROUNDTRUTH_DIR = cfg.get('directory', 'groundtruth_dir')


def generate_tmp(segments=ALL_SEGMENTS, to_dir="tmp"):
    print('generating json files ...')
    ctr = 0
    sys.stdout.write("\r%d/%d" % (ctr, len(segments)))
    sys.stdout.flush()

    for segment in segments:
        data = load_segment(segment['dir'])
        asa = ASAnalyzer(SETTINGS_PRESET['GENERAL'])
        deviation = 0
        seg_features = []
        for i in range(data.shape[0]):
            asa.add_data(data[i])
            if len(asa.delta) % 5 == 0 and len(asa.delta) != 0:
                if len(asa.delta) == 5:
                    deviation = len(asa.raw) - len(asa.delta) - 1
                    assert deviation == asa.first_offset
                seg_features.append(asa.extract_window_features(5))

        points = []
        for index, cand in enumerate(asa.candidates):
            if len(cand) == 0:
                continue
            else:
                for cad in cand:
                    points.append((index, int(cad[2]), int(cad[1])))

        seg_dict = {
            'name': segment['name'],
            'deviation': deviation,
            'raw': data.tolist(),
            'delta': [dat.tolist() for dat in asa.delta],
            'feature': seg_features,
            'c1': segment['point1'],
            'c2': segment['point2'],
            'points': points,
            'trace': asa.user_trace,
        }

        if not os.path.isdir(to_dir):
            os.mkdir(to_dir)
        with open(os.path.join(to_dir, segment['name'] + '.json'), 'w') as fp:
            json_data = json.dump(seg_dict, fp)

        ctr += 1
        sys.stdout.write("\r%d/%d" % (ctr, len(ALL_SEGMENTS)))
        sys.stdout.flush()
    print('done!')


def load_tmp_file(name, tmp_dir='tmp'):
    with open(os.path.join(tmp_dir, name + '.json'), 'r') as fp:
        json_data = json.load(fp)
    return json_data

if  __name__ == '__main__':
    generate_tmp()


