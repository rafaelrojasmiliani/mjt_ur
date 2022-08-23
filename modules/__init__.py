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
MOD_PATH_DMSUITE = pathlib.Path(MOD_PATH, 'dmsuite')
MOD_PATH_GSPLINES = pathlib.Path(MOD_PATH, 'gsplines')
MOD_PATH_GSPLINES_OPT = pathlib.Path(MOD_PATH, 'gspline_optimizer')
MOD_PATH_VSDK = pathlib.Path(MOD_PATH, 'vsdk')
MOD_PATH_VSURT = pathlib.Path(MOD_PATH, 'vstoolsur')
sys.path.append(str(MOD_PATH_DMSUITE))
sys.path.append(str(MOD_PATH_GSPLINES))
sys.path.append(str(MOD_PATH_GSPLINES_OPT))
sys.path.append(str(MOD_PATH_VSDK))
sys.path.append(str(MOD_PATH_VSURT))

import dmsuite
import gsplines
import gsplinesopt

#import vsdk

#import vsurt
