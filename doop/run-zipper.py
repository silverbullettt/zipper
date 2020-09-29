#!/usr/bin/env python
import os
import shutil
import sys
import multiprocessing

# This script should be executed from the root directory of Doop.

# ----------------- configuration -------------------------
DOOP = './run --color '
PRE_ANALYSIS = 'context-insensitive'
DATABASE = 'last-analysis'
ANALYSES = [
    'context-insensitive',
    '2-object-sensitive+heap',
    '2-call-site-sensitive+heap',
    '2-type-sensitive+heap',          
    'refA-2-object-sensitive+heap',
    'refA-2-type-sensitive+heap',
    'refA-2-call-site-sensitive+heap',
    'refB-2-object-sensitive+heap',
    'refB-2-type-sensitive+heap',
    'refB-2-call-site-sensitive+heap',
]

APP = 'temp'

ZIPPER_HOME = '../zipper'
ZIPPER_CP = ':'.join([
    os.path.join(ZIPPER_HOME, 'build', 'zipper.jar'),
    os.path.join(ZIPPER_HOME, 'lib', 'guava-23.0.jar'),
    os.path.join(ZIPPER_HOME, 'lib', 'sootclasses-2.5.0.jar'),
])
ZIPPER_MAIN = 'ptatoolkit.zipper.Main'
ZIPPER_PTA = 'ptatoolkit.zipper.doop.DoopPointsToAnalysis'
ZIPPER_CACHE = 'cache/zipper'
ZIPPER_OUT = 'results'

ZIPPER_THREAD = multiprocessing.cpu_count() # use multithreading to accelerate Zipper
ZIPPER_MEMORY = '48g'

# ---------------------------------------------------------

RESET = '\033[0m'
YELLOW = '\033[33m'
BOLD = '\033[1m'

def runPreAnalysis(initArgs):
    args = [DOOP] + [a if a not in ANALYSES else PRE_ANALYSIS for a in initArgs]
    cmd = ' '.join(args)
    print YELLOW + BOLD + 'Running pre-analysis ...' + RESET
    # print cmd
    os.system(cmd)

QUERY={
    # points-to set
    'VPT':"Stats:Simple:InsensVarPointsTo",

    # object
    'OBJ':"_(obj)<-Stats:Simple:InsensVarPointsTo(obj,_).",
    'OBJ_TYPE':"_[obj]=type<-" +
                "HeapAllocation:Type[obj]=type," +
                "Stats:Simple:InsensVarPointsTo(obj,_).",
    'SPECIAL_OBJECTS':"HeapAllocation:Special",
    'MERGE_OBJECTS':"HeapAllocation:Merge",
    'STRING_CONSTANTS':"StringConstant",
    'REF_STRING_CONSTANTS':"ReflectionStringConstant",
    
    'OBJECT_IN':"_(obj,inmethod)<-" +
            "AssignHeapAllocation(obj,_,inmethod)," +
            "Reachable(inmethod).",
    'OBJECT_ASSIGN':"_(obj,var)<-" +
            "AssignHeapAllocation(obj,var,inmethod)," +
            "Reachable(inmethod).",
    'REF_OBJECT':"_(obj,callsite)<-" +
                "ReflectiveHeapAllocation[callsite,_]=obj.",

    'STRING_CONSTANT':"<<string-constant>>",

    # flow graph
    'LOCAL_ASSIGN':"_(to,from)<-OptAssignLocal(to,from);" +
            "Assign(_,_,to,_,from).",
    'INTERPROCEDURAL_ASSIGN':"_(to,from)<-" +
            "OptInterproceduralAssign(_,to,_,from).",
    'INSTANCE_LOAD':"_(to,base,field)<-" +
            "LoadHeapInstanceField(_,to,field,_,base).",
    'ARRAY_LOAD':"_(to,array)<-LoadHeapArrayIndex(_,to,_,array).",
    'INSTANCE_STORE':"_(base,field,from)<-" +
            "StoreHeapInstanceField(field,_,base,_,from).",
    'ARRAY_STORE':"_(array,from)<-StoreHeapArrayIndex(_,array,_,from).",
    'INSTANCE_LOAD_FROM_TO':"_(from,to)<-" +
            "ReachableLoadInstanceFieldBase(from)," +
            "OptLoadInstanceField(to,_,from).",
    'ARRAY_LOAD_FROM_TO':"_(from,to)<-" +
            "ReachableLoadArrayIndexBase(from)," +
            "OptLoadArrayIndex(to,from).",
    'CALL_RETURN_TO':"_(recv,to)<-" +
            "VirtualMethodInvocation:Base[invo]=recv," +
            "VirtualMethodInvocation:In(invo,inmethod)," +
            "AssignReturnValue[invo]=to," +
            "Reachable(inmethod).",
    # call graph edges
    'REGULARCALL':"Stats:Simple:InsensCallGraphEdge",
    'REFCALL':"_(from,to)<-ReflectiveCallGraphEdge(_,from,_,to).",
    'NATIVECALL':"_(from,to)<-NativeCallGraphEdge(_,from,_,to).",
    'CALL_EDGE':"Stats:Simple:WholeInsensCallGraphEdge",
    'CALLER_CALLEE':"_(caller,callee)<-" +
            "(Stats:Simple:InsensCallGraphEdge(callsite,callee);" +
            "ReflectiveCallGraphEdge(_,callsite,_,callee))," +
            "(SpecialMethodInvocation:In(callsite,caller);" +
            "VirtualMethodInvocation:In(callsite,caller);" +
            "StaticMethodInvocation:In(callsite,caller)).",
    'MAINMETHOD':"MainMethodDeclaration",
    'REACHABLE':"Reachable",
    'IMPLICITREACHABLE':"ImplicitReachable",

    # instance field store
    'INSTANCE_STORE_IN':"_(obj,inmethod)<-" +
            "ReachableStoreInstanceFieldBase(base)," +
            "VarPointsTo(_,obj,_,base)," +
            "Var:DeclaringMethod(base,inmethod).",
    'ARRAY_STORE_IN':"_(array,inmethod)<-" +
            "ReachableStoreArrayIndexBase(base)," +
            "VarPointsTo(_,array,_,base)" +
            "Var:DeclaringMethod(base,inmethod).",

    # call site
    'INST_CALL':"_(callsite,callee)<-" +
            "Stats:Simple:InsensCallGraphEdge(callsite,callee)," +
            "(VirtualMethodInvocation(callsite,_,_);" +
            "SpecialMethodInvocation:Base[callsite]=_).",
    'INST_CALL_RECV':"_(callsite,recv)<-" +
            "Stats:Simple:InsensCallGraphEdge(callsite,_)," +
            "(VirtualMethodInvocation:Base[callsite]=recv;" +
            "SpecialMethodInvocation:Base[callsite]=recv).",
    'INST_CALL_ARGS':"_(callsite,arg)<-" +
            "Stats:Simple:InsensCallGraphEdge(callsite,_)," +
            "(VirtualMethodInvocation(callsite,_,_);" +
            "SpecialMethodInvocation:Base[callsite]=_)," +
            "ActualParam[_,callsite]=arg.",
    'CALLSITEIN':"MethodInvocation:In",

    # method
    'THIS_VAR':"_(mtd,this)<-Reachable(mtd),ThisVar[mtd]=this.",
    'PARAMS':"_(mtd,param)<-Reachable(mtd),FormalParam[_,mtd]=param.",
    'RET_VARS':"_(mtd,ret)<-Reachable(mtd),ReturnVar(ret,mtd).",
    # only instance methods have this variables
    'INST_METHODS':"_(mtd)<-Reachable(mtd),ThisVar[mtd]=_.",
    'OBJFINAL':"ObjectSupportsFinalize",
    'VAR_IN':"_(var,inmethod)<-Var:DeclaringMethod(var,inmethod)," +
            "Reachable(inmethod).",
    'METHOD_MODIFIER':"_(mtd,mod)<-MethodModifier(mod,mtd).",

    # type
    'APPLICATION_CLASS':"ApplicationClass",
    'DIRECT_SUPER_TYPE':"DirectSuperclass",
    'DECLARING_CLASS_ALLOCATION':"DeclaringClassAllocation",
}

DB_DIR = None
DUMP_DIR = None
def dumpQuery(query):
    output = os.path.join(DUMP_DIR, '%s.%s' % (APP, query))
    if os.path.exists(output):
        os.remove(output) # remove old file
    cmd = "bloxbatch -db %s -query '%s' > %s" % (DB_DIR, QUERY[query], output)
    # print cmd
    os.system(cmd)

def dumpRequiredDoopResults(app, db_dir, dump_dir):
    print 'Dumping doop analysis results ...'
    REQUIRED_QURIES = [
        'ARRAY_LOAD', 'ARRAY_STORE',
        'CALL_EDGE', 'CALL_RETURN_TO', 'CALLSITEIN', 'DIRECT_SUPER_TYPE',
        'INST_CALL_RECV', 'INST_METHODS', 'INSTANCE_LOAD', 'INSTANCE_STORE', 
        'INTERPROCEDURAL_ASSIGN', 'LOCAL_ASSIGN', 'METHOD_MODIFIER',
        'OBJ_TYPE', 'OBJECT_ASSIGN', 'OBJECT_IN',
        'PARAMS', 'RET_VARS', 'SPECIAL_OBJECTS',
        'THIS_VAR', 'VAR_IN', 'VPT',
    ]
    if not os.path.isdir(dump_dir):
        os.makedirs(dump_dir)
    global APP, DB_DIR, DUMP_DIR
    APP, DB_DIR, DUMP_DIR = app, db_dir, dump_dir
    n = min(len(REQUIRED_QURIES), multiprocessing.cpu_count())
    pool = multiprocessing.Pool(n)
    # dump queries with multiprocesses
    pool.map(dumpQuery, REQUIRED_QURIES)
    pool.close()
    pool.join()

def runZipper(app, cache_dir, out_dir):
    zipper_file = os.path.join(out_dir, \
        '%s-ZipperPrecisionCriticalMethod.facts' % app)
    if os.path.exists(zipper_file):
        os.remove(zipper_file) # remove old file

    cmd = 'java -Xmx%s ' % ZIPPER_MEMORY
    cmd += ' -cp %s ' % ZIPPER_CP
    cmd += ZIPPER_MAIN
    cmd += ' -pta %s ' % ZIPPER_PTA
    cmd += ' -app %s ' % app
    cmd += ' -cache %s ' % cache_dir
    cmd += ' -out %s ' % out_dir
    if ZIPPER_THREAD > 1:
        cmd += ' -thread %d ' % ZIPPER_THREAD
    # print cmd
    os.system(cmd)
    return zipper_file

def runMainAnalysis(args, zipper_file):
    args = [DOOP, '--cache', '-zipper', zipper_file] + args
    cmd = ' '.join(args)
    print YELLOW + BOLD + 'Running main (Zipper-guided) analysis ...' + RESET
    # print cmd
    os.system(cmd)

def run(args):
    runPreAnalysis(args)
    dumpRequiredDoopResults(APP, DATABASE, ZIPPER_CACHE)
    zipper_file = runZipper(APP, ZIPPER_CACHE, ZIPPER_OUT)
    runMainAnalysis(args, zipper_file)

if __name__ == '__main__':
    run(sys.argv[1:])
