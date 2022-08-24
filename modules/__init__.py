'''
This file import the packages that are git submodules
'''
import sys
import os
import pathlib
print('------------------')
print('------------------')
print('------------------')
print('------------------')
MOD_PATH = pathlib.Path(__file__).parent.absolute()
MOD_PATH_GSPLINES = pathlib.Path(MOD_PATH, 'gsplines')
MOD_PATH_GSPLINES_OPT = pathlib.Path(MOD_PATH, 'gspline_optimizer')
sys.path.append(str(MOD_PATH_GSPLINES))
sys.path.append(str(MOD_PATH_GSPLINES_OPT))

import gsplines
import gsplinesopt

#import vsdk

#import vsurt
