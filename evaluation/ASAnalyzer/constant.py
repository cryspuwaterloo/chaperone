# Default settings for Google Pixel
SLOT_3_LEAVING = [
    [(2, 0, 0), (2, 1, 1), (2, 0, 1)],
    [(2, 0, 0), (0, 0, 0), (2, 1, 0)]
]


# Specified Settings

SETTINGS_PRESET = {
    'GENERAL': {
        # Basic settings for signal processing side
        'sampling_rate': 48000,
        'period': 2400,
        'sound_speed': 340,
        'down_sample': 4,
        # Detector settings
        'window_size': 5,
        'slot_size': 3,
        'direct_high_pass': 3000,
        'direct_high_bound': 4000,
        'direct_low_pass': 150,
        # Outlier detection
        'outlier_mul': 3,
        'outlier_p': 94,
        'cluster_tolerance': 3,
        'max_movement': 30,
        'min_movement': 5,
        'discount': 0.95,
        # Noise exclusion
        'far_index':80,
        'far_noise': 400,
        'far_diff_noise': 180,
        'glitch_noise': 6000,
        'glitch_size': 11,
        'extreme_near':3,
        'start_index': 12,
        # Kalman filter
        'dk_R': 0.25,
        'dk_Q': 1e-3,
        'dk_x0': 30, # assume the distance between user and phone is 0.21 m
        # Step function
        'min_theta': [
            (0.2, 400),
            (0.4, 320),
            (0.6, 270),
            (0.8, 200),
            (1, 180),
            (1.5, 150),
            (1.8, 120),
            (2.5, 100)
        ],
        'raw_theta': [
        (0.5, 600),
        (1.5, 450),
        # (3, 350),
        # (4, 200)
        ],
    },
}

DEFAULT_SETTINGS = SETTINGS_PRESET['GENERAL']
