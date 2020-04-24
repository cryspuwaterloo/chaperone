import os
import numpy as np
import pandas as pd


def load_menu(file, parent=None):
    with open(file, 'r') as fp:
        menu = []
        for line in fp:
            elements = line.strip().split(' ')
            # First parser
            data_file = elements[0]
            st = int(elements[1])
            ed = int(elements[2])
            lp1 = int(elements[3])
            lp2 = int(elements[4])
            # Second parser
            disp_name = data_file.split('/')[-1].split('.')[0]
            file_name = data_file if parent is None else os.path.join(
                parent, data_file.split('/')[-1]
            )
            menu.append({
                'name': disp_name,
                'dir': file_name,
                'start': st,
                'end': ed,
                'point1': lp1,
                'point2': lp2,
            })
    return menu


def load_segment(file):
    with open(file, 'r') as fp:
        data = [[int(float(elem)) for elem in line.strip().split(',')]
                for line in fp]
        return np.array(data)


def load_raw_file(file, delimiter='\t'):
    with open(file, 'r') as fp:
        data = [[float(elem) for elem in line.strip().split(delimiter)]
                for line in fp]
        return np.array(data)

