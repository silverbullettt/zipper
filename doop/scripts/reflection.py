#!/usr/bin/python

__author__ = "Tian Tan and Yue Li"

import os, sys
import re

INVOCATIONREF = 'MethodInvocationRef.facts'
PATTERN = re.compile('<(?P<clsName>[^:]+): \S+ (?P<methName>[^(]+).*')
REFCALL = {
 #'Class.forName':'java.lang.Class.forName',
 'Class.newInstance':'java.lang.Class.newInstance',
 'Constructor.newInstance':'java.lang.reflect.Constructor.newInstance',
 #'Field.get*':'java.lang.reflect.Field.get',
 #'Field.set*':'java.lang.reflect.Field.set',
 'Method.invoke':'java.lang.reflect.Method.invoke',
}

def convertToDotFormat(caller):
 if caller.startswith('<'):
  match = PATTERN.match(caller)
  d = match.groupdict()
  return '%s.%s' % (d['clsName'], d['methName'])
 else:
  return caller

def isPotentialCall(call, caller, invo):
 invoCaller, invoCallee, _ = invo.split('/')
 return caller == convertToDotFormat(invoCaller) and \
  REFCALL[call] == invoCallee

def findInvocations(call, caller, invos):
 return [invo for invo in invos if isPotentialCall(call, caller, invo)]

def filterReflectiveInvocation(invos):
 refInvos = set()
 for invo in invos:
  for _, call in REFCALL.items():
   if call in invo and \
     'java.lang.Class.newInstance0' not in invo: # skip wrapper call
    refInvos.add(invo.strip())
 return refInvos
 
def convertReflectionLog(refLog, factsDir, outDir):
 flog = open(refLog)
 
 finvo = open(os.path.join(factsDir, INVOCATIONREF))
 refInvos = filterReflectiveInvocation(finvo.readlines())
 finvo.close()
 
 outputDict = {
  #'Class.forName':open(os.path.join(outDir, 'ClassForName.log'), 'w'),
  'Class.newInstance':open(os.path.join(outDir, 'ClassNewInstance.log'), 'w'),
  'Constructor.newInstance':open(os.path.join(outDir, 'ConstructorNewInstance.log'), 'w'),
  #'Field.get*':open(os.path.join(outDir, 'FieldGet.log'), 'w'),
  #'Field.set*':open(os.path.join(outDir, 'FieldSet.log'), 'w'),
  'Method.invoke':open(os.path.join(outDir, 'MethodInvoke.log'), 'w'),
 }
 for line in flog:
  call, target, caller, _, _, _ = line.split(';')
  # skip generated accessors
  if 'GeneratedConstructorAccessor' in target or \
    'GeneratedMethodAccessor' in target:
   continue
  # skip non side-effect calls
  if call not in outputDict.keys():
   continue
  output = outputDict[call]
  for invo in findInvocations(call, caller, refInvos):
   output.write('%s\t%s\n' % (invo, target))
 flog.close()
 for _, output in outputDict.items():
  output.close()

def run(args):
 [refLog, factsDir, outDir] = args
 convertReflectionLog(refLog, factsDir, outDir)

if __name__ == '__main__':
 run(sys.argv[1:])
