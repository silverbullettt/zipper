#!/usr/bin/env python
import os
import sys

QUERY={
    # points-to set
    'VPT':"Stats:Simple:InsensVarPointsTo",

    # field points-to graph
	'OBJ':"_(obj)<-Stats:Simple:InsensVarPointsTo(obj,_).",
	'OBJ_TYPE':"_[obj]=type<-" +
                "HeapAllocation:Type[obj]=type," +
                "Stats:Simple:InsensVarPointsTo(obj,_).",
	'IFPT':"_(baseobj,field,obj)<-" +
                "InstanceFieldPointsTo(_,obj,field,_,baseobj).",
	'APT':"_(array,obj)<-ArrayIndexPointsTo(_,obj,_,array).",

    # object
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
	# currently not considering special calls
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

REQUIRED_QURIES={
    'scaler':[
        'INST_METHODS', 'SPECIAL_OBJECTS', 'APPLICATION_CLASS', 'VPT', 'VAR_IN',
        'CALLSITEIN', 'CALL_EDGE', 'DECLARING_CLASS_ALLOCATION', 'OBJECT_IN',
    ],
    'zipper':[
        'LOCAL_ASSIGN', 'INTERPROCEDURAL_ASSIGN', 'INSTANCE_LOAD', 'ARRAY_LOAD',
        'INSTANCE_STORE', 'ARRAY_STORE', 'INST_METHODS', 'CALL_RETURN_TO',
        'SPECIAL_OBJECTS', 'VPT', 'OBJECT_IN', 'CALLSITEIN', 'CALL_EDGE',
        'VAR_IN', 'OBJECT_ASSIGN', 'IFPT', 'APT', 'DIRECT_SUPER_TYPE',
    ],
}

def dumpDoopResults(db_dir, dump_dir, app, query):
    output = os.path.join(dump_dir, '%s.%s' % (app, query))
    if not os.path.exists(output):
        cmd = "bloxbatch -db %s -query '%s' > %s" % (db_dir, QUERY[query], output)
        # print cmd
        os.system(cmd)

def dumpRequiredDoopResults(app, analysis, db_dir, dump_dir):
    print 'Dumping doop analysis results for %s ...' % app
    for query in REQUIRED_QURIES[analysis]:
        dumpDoopResults(db_dir, dump_dir, app, query)

if __name__ == '__main__':
    dumpRequiredDoopResults('luindex', 'scaler', 'results/context-insensitive/jre1.6/luindex.jar', 'results')
