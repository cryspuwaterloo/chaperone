# Chaperone Evaluation Scripts

This repository provides the evaluation scripts for experiments in the Usenix
Security submission *Chaperone: Real-time Locking and Loss Prevention for
Smartphones*. You can use the scripts to reproduce the experimental results in
the paper. We also provide a python version of Chaperone's user tracking module,
called ASAnalyzer.


## Installation

The evaluation scripts require Python 3.6 or later versions. They may also require
the following libraries:

- Numpy
- Scipy
- Pandas
- Scikit-learn

Please install the latest version of these dependencies before running the scripts.

## Dataset setup

We provide our experiment dataset in URL.
The dataset includes two parts: 1. lab experiments (note as ```lab```) 2. real-
world experiments (```real_world```).
The groundtruth of lab experiments are stored in the folder ```groundtruth```.
Please unzip the dataset and copy all three folders under the ```dataset``` folder.
Note: if you change any folder name, please change the related fields
in ```setting.ini``` file accordingly.

## Basic usage

### Step 1: Generate tmp files

Generating tmp files is actually running Chaperone's user tracking module on
all data records in the dataset. It produces the intermediate files (including
the estimated traces and extracted features) for evaluation.

To generate tmp files, you just need to run the following code:
```bash
$ python initialize.py
```

The generated files are stored as ```.json``` files in ```tmp``` folder.

### Step 2: Run evaluation scripts

After generating all necessary files, you are able to run the experiments with
the following code:

```bash
$ python evaluation.py <EXPERIMENT_CODE>
```

Please replace ```<EXPERIMENT_CODE>``` with one of the following experiment codes:

- ```lab_speed_angle```: Lab experiments - different phone orientation angles and user departing speeds
- ```lab_nearby```: Lab experiments - impact from a nearby stranger at different distances
- ```real_world_overall```: Real-world experiments - overall results
- ```real_world_per_user```: Real-world experiments - per user results
- ```real_world_per_location```: Real-world experiments - per location results

Note: ```lab_speed_angle``` may take several minutes in the training stage.

## Advanced usage - ASAnalyzer

ASAnalyzer is a python version of Chaperone that designed for remote analysis.
It receives the filtered acoustic signal to produces the current distance
between the user and the device. If you want to rerun the experiments with your
own data or settings,  we encourage you to make changes in this module. Here is
a brief guide to its usage.

### Setup an ASAnalyzer

Please make sure the directory to ASAnalyzer is included in your current Python
project. Then import ASAnalyzer in your script. You can simply use it with the
following sample code:

```Python
from ASAnalyzer import ASAnalyzer
import numpy as np

# data, data2, data3 are lists or arrays with the same size
length = 150
scale = 100
data = np.random.rand(length) * scale
data2 = np.random.rand(length) * scale
data3 = np.random.rand(length) * scale

asa = ASAnalyzer()
asa.add_data(data)
asa.add_data(data2)
asa.add_data(data3)

# Usually, random vectors will be regarded as noise by Chaperone 
# There is no valid user trace in this example
# Please use our public dataset for a try

print(asa.user_trace) # get the user trace in a list of (time, distance)
```

The input of ```add_data``` is a vector of filtered acoustic data (i.e., a frame).
Each vector mush have the same shape. Please refer to our dataset for the data format.

After obtaining sufficient data (i.e., after five frame), you can
use ```extract_window_features(window_size)``` to extract a feature vector.


### Tune tracker parameters

In ASAnalyzer package, we provide our tracker parameters in ```constant.py```.
You can change the parameters or make your own settings in this file. Here,
we provide a series of parameters that are usually tuned in our project.

If you adopt a different sensing scheme, you may need to first change acoustic
sensing related parameters:

- ```sampling_rate```: sampling rate of recording in Hz, i.e., 48000, 44100
- ```period```: sensing period in samples, it includes both the acoustic sending
period and the signal receiving period, i.e., the time interval between two signals

If you want to try a different different outlier detection parameters, you may
need to change ```outlier_mul```, which is the coefficient for MAD outlier 
detection. ```cluster_tolerance``` determines the maximum distance between
two candidates that belong to the same cluster.

For Kalman filter, you can change the parameters ```R``` and ```Q```.


## License
MIT License

