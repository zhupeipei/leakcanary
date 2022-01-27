package com.example.mydemo

import android.Manifest
import android.os.Bundle
import android.os.Debug
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import leakcanary.internal.AndroidDebugHeapAnalyzer
import shark.*

const val TAG = "demoDump"

const val BITMAP_CLASS_NAME = "android.graphics.Bitmap"

private const val DEFAULT_BIG_PRIMITIVE_ARRAY = 256 * 1024
private const val DEFAULT_BIG_BITMAP = 768 * 1366 + 1
private const val DEFAULT_BIG_OBJECT_ARRAY = 256 * 1024
private const val SAME_CLASS_LEAK_OBJECT_PATH_THRESHOLD = 45

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    requestPermissions(
      arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
      ), 1
    )
  }

  fun hprofAnalyze(view: View) {
    val heapDumpFile = "${Environment.getExternalStorageDirectory()}/1.hprof"
    Log.i(TAG, "hprofAnalyze: $heapDumpFile")
    // Prints all thread names
    Hprof.open(File(heapDumpFile))
      .use { hprof ->
        Log.i(TAG, "hprofAnalyze: start")
        val mHeapGraph = HprofHeapGraph.indexHprof(hprof)

        val bitmapHeapClass = mHeapGraph.findClassByName(BITMAP_CLASS_NAME)

        //缓存classHierarchy，用于查找class的所有instance
        val classHierarchyMap = mutableMapOf<Long, Pair<Long, Long>>()
        //记录class objects数量
        val classObjectCounterMap = mutableMapOf<Long, ObjectCounter>()

        //遍历镜像的所有instance
        for (instance in mHeapGraph.instances) {
          if (instance.isPrimitiveWrapper) {
            continue
          }

          //使用HashMap缓存及遍历两边classHierarchy，这2种方式加速查找instance是否是对应类实例
          //superId1代表类的继承层次中倒数第一的id，0就是继承自object
          //superId4代表类的继承层次中倒数第四的id
          //类的继承关系，以AOSP代码为主，部分厂商入如OPPO Bitmap会做一些修改，这里先忽略
          val instanceClassId = instance.instanceClassId
          val (superId1, superId4) = if (classHierarchyMap[instanceClassId] != null) {
            classHierarchyMap[instanceClassId]!!
          } else {
            val classHierarchyList = instance.instanceClass.classHierarchy.toList()

            val first = classHierarchyList.getOrNull(classHierarchyList.size - 2)?.objectId ?: 0L
            val second = classHierarchyList.getOrNull(classHierarchyList.size - 5)?.objectId ?: 0L

            Pair(first, second).also { classHierarchyMap[instanceClassId] = it }
          }


          //Bitmap
          if (bitmapHeapClass?.objectId == superId1) {
            val fieldWidth = instance[BITMAP_CLASS_NAME, "mWidth"]
            val fieldHeight = instance[BITMAP_CLASS_NAME, "mHeight"]
            val width = fieldWidth!!.value.asInt!!
            val height = fieldHeight!!.value.asInt!!


            if (width == 1920 && height == 1080) {
              Log.i(TAG, "hprofAnalyze: ${instance.instanceClassName}")

              Log.i(TAG, "hprofAnalyze bitmap $width + $height")

              val classHierarchyList = instance.instanceClass.classHierarchy.toList()
              printHeapClass(classHierarchyList)


              val progressListener = OnAnalysisProgressListener { step ->
                val percent = (step.ordinal * 1.0) / OnAnalysisProgressListener.Step.values().size
                Log.i(TAG, "hprofAnalyze: $percent")
              }

              val heapDumpFile = File(heapDumpFile)

              myWaitDebugListener = Runnable {
                Log.i(TAG, "hprofAnalyze: paused waiting for debug")
                Debug.waitForDebugger()
              }

              val heapAnalysis = AndroidDebugHeapAnalyzer.analyzeHeap(heapDumpFile, progressListener, { false }, instance.objectId)

              Log.i(TAG, "hprofAnalyze: end1")

              Debug.waitForDebugger()

              val count = 1

              Log.i(TAG, "hprofAnalyze: $heapAnalysis")

//              findPathsToGcRoot(File(heapDumpFile), mHeapGraph, instance)


//              val heapAnalyzer = HeapAnalyzer(AnalyzerProgressListener.NONE)
//              val analysis = heapAnalyzer.analyze(
//                heapDumpFile = heapDumpFile,
//                graph = heapGraph,
//                leakingObjectFinder = leakingObjectFinder,
//              )
//
//              // Marks any instance of com.example.ThingWithLifecycle with
//              // ThingWithLifecycle.destroyed=true as leaking
//              val leakingObjectFilter = object : FilteringLeakingObjectFinder.LeakingObjectFilter {
//                override fun isLeakingObject(heapObject: HeapObject): Boolean {
//                  return if (heapObject == "com.example.ThingWithLifecycle") {
//                    val instance = heapObject as HeapObject.HeapInstance
//                    val destroyedField = instance["com.example.ThingWithLifecycle", "destroyed"]!!
//                    destroyedField.value.asBoolean!!
//                  } else false
//                }
//              }
//
//              val leakingObjectFinder = FilteringLeakingObjectFinder(listOf(leakingObjectFilter))
//
//              val heapAnalysis = Hprof.open(heapDumpFile)
//                .use { hprof ->
//                  val heapGraph = HprofHeapGraph.indexHprof(hprof)
//                  val heapAnalyzer = HeapAnalyzer(AnalyzerProgressListener.NONE)
//                  heapAnalyzer.analyze(
//                    heapDumpFile = heapDumpFile,
//                    graph = heapGraph,
//                    leakingObjectFinder = leakingObjectFinder,
//                  )
//                }
//              println(analysis)
//
//
//
//              mHeapGraph.
//
//              val helpers =
//                FindLea(graph, referenceMatchers, computeRetainedHeapSize, objectInspectors)
//              helpers.analyzeGraph(
//                metadataExtractor, leakingObjectFinder, heapDumpFile, analysisStartNanoTime
            }




//            mHeapGraph.findObjectById(classHierarchyList[0].objectId)



            if (width * height >= DEFAULT_BIG_BITMAP) {

              val objectCounter = updateClassObjectCounterMap(classObjectCounterMap, instanceClassId, true)
//              Log.e(TAG, "suspect leak! bitmap name: ${instance.instanceClassName}" +
//                " width: ${width} height:${height}")



//              if (objectCounter.leakCnt <= SAME_CLASS_LEAK_OBJECT_PATH_THRESHOLD) {
//                mLeakingObjectIds.add(instance.objectId)
//                mLeakReasonTable[instance.objectId] = "Bitmap Size Over Threshold, ${width}x${height}"
//                Log.i(TAG,
//                  instance.instanceClassName + " objectId:" + instance.objectId)
//
//                //加入大对象泄露json
//                val leakObject = HeapReport.LeakObject().apply {
//                  className = instance.instanceClassName
//                  size = (width * height).toString()
//                  extDetail = "$width x $height"
//                  objectId = (instance.objectId and 0xffffffffL).toString()
//                }
//                mLeakModel.leakObjects.add(leakObject)
//              }
            }
            continue
          }
        }

        Log.i(TAG, "hprofAnalyze: end")


        val threadClass = mHeapGraph.findClassByName("java.lang.Thread")!!
        val threadNames: Sequence<String> = threadClass.instances.map { instance: HeapObject.HeapInstance ->
          val nameField = instance["java.lang.Thread", "name"]!!
          nameField.value.readAsJavaString()!!
        }
        threadNames.forEach { println(it) }
      }
  }

//  private fun findPathsToGcRoot(file: File, mHeapGraph: HeapGraph, instance: HeapObject.HeapInstance) {
//    val startTime = System.currentTimeMillis()
//
//    val heapAnalyzer = HeapAnalyzer(
//      OnAnalysisProgressListener { step: OnAnalysisProgressListener.Step ->
//        Log.i(TAG, "step:" + step.name + ", leaking obj size:" + instance.objectId)
//      }
//    )
//
//    AndroidDebugHeapAnalyzer.runAnalysisBlocking(
//
//    )
//    heapAnalyzer.analyze(file, mHeapGraph, )
//
//    val referenceReader = DelegatingObjectReferenceReader(
//      classReferenceReader = ClassReferenceReader(mHeapGraph, referenceMatchers),
//      instanceReferenceReader = ChainingInstanceReferenceReader(
//        listOf(
//          JavaLocalReferenceReader(graph, referenceMatchers),
//        )
//          + OpenJdkInstanceRefReaders.values().mapNotNull { it.create(graph) }
//          + ApacheHarmonyInstanceRefReaders.values().mapNotNull { it.create(graph) }
//          + AndroidReferenceReaders.values().mapNotNull { it.create(graph) },
//        FieldInstanceReferenceReader(graph, referenceMatchers)
//      ),
//      objectArrayReferenceReader = ObjectArrayReferenceReader()
//    )
//
//    val findLeakInput = HeapAnalyzer.FindLeakInput(
//      mHeapGraph, AndroidReferenceMatchers.appDefaults,
//      false, mutableListOf(), referenceReader
//    )
//
//
//    val (applicationLeaks, libraryLeaks) = with(heapAnalyzer) {
//      findLeakInput.findLeaks(setOf(instance.objectId))
//    }
//
//    Log.i(TAG,
//      "---------------------------Application Leak---------------------------------------")
//    //填充application leak
//    Log.i(TAG, "ApplicationLeak size:" + applicationLeaks.size)
//    for (applicationLeak in applicationLeaks) {
//      Log.i(TAG, "shortDescription:" + applicationLeak.shortDescription
//        + ", signature:" + applicationLeak.signature
//        + " same leak size:" + applicationLeak.leakTraces.size
//      )
//
//      val (gcRootType, referencePath, leakTraceObject) = applicationLeak.leakTraces[0]
//
//      val gcRoot = gcRootType.description
//      val labels = leakTraceObject.labels.toTypedArray()
////      leakTraceObject.leakingStatusReason = mLeakReasonTable[leakTraceObject.objectId].toString()
//
//      Log.i(TAG, "GC Root:" + gcRoot
//        + ", leakObjClazz:" + leakTraceObject.className
//        + ", leakObjType:" + leakTraceObject.typeName
//        + ", labels:" + labels.contentToString()
//        + ", leaking reason:" + leakTraceObject.leakingStatusReason
//        + ", leaking obj:" + "null")
//
////      val leakTraceChainModel = HeapReport.GCPath()
////        .apply {
////          this.instanceCount = applicationLeak.leakTraces.size
////          this.leakReason = leakTraceObject.leakingStatusReason
////          this.gcRoot = gcRoot
////          this.signature = applicationLeak.signature
////        }
////        .also { mLeakModel.gcPaths.add(it) }
//
//      // 添加索引到的trace path
//      for (reference in referencePath) {
//        val referenceName = reference.referenceName
//        val clazz = reference.originObject.className
//        val referenceDisplayName = reference.referenceDisplayName
//        val referenceGenericName = reference.referenceGenericName
//        val referenceType = reference.referenceType.toString()
//        val declaredClassName = reference.owningClassName
//
//        Log.i(TAG, "clazz:" + clazz +
//          ", referenceName:" + referenceName
//          + ", referenceDisplayName:" + referenceDisplayName
//          + ", referenceGenericName:" + referenceGenericName
//          + ", referenceType:" + referenceType
//          + ", declaredClassName:" + declaredClassName)
//
////        val leakPathItem = HeapReport.GCPath.PathItem().apply {
////          this.reference = if (referenceDisplayName.startsWith("["))  //数组类型[]
////            clazz
////          else
////            "$clazz.$referenceDisplayName"
////          this.referenceType = referenceType
////          this.declaredClass = declaredClassName
////        }
//
////        leakTraceChainModel.path.add(leakPathItem)
//      }
//
//      // 添加本身trace path
////      leakTraceChainModel.path.add(HeapReport.GCPath.PathItem().apply {
////        reference = leakTraceObject.className
////        referenceType = leakTraceObject.typeName
////      })
//    }
//    Log.i(TAG, "=======================================================================")
//    Log.i(TAG, "----------------------------Library Leak--------------------------------------");
//    //填充library leak
//    Log.i(TAG, "LibraryLeak size:" + libraryLeaks.size)
//    for (libraryLeak in libraryLeaks) {
//      Log.i(TAG, "description:" + libraryLeak.description
//        + ", shortDescription:" + libraryLeak.shortDescription
//        + ", pattern:" + libraryLeak.pattern.toString())
//
//      val (gcRootType, referencePath, leakTraceObject) = libraryLeak.leakTraces[0]
//      val gcRoot = gcRootType.description
//      val labels = leakTraceObject.labels.toTypedArray()
////      leakTraceObject.leakingStatusReason = mLeakReasonTable[leakTraceObject.objectId].toString()
//
//      Log.i(TAG, "GC Root:" + gcRoot
//        + ", leakClazz:" + leakTraceObject.className
//        + ", labels:" + labels.contentToString()
//        + ", leaking reason:" + leakTraceObject.leakingStatusReason)
//
////      val leakTraceChainModel = HeapReport.GCPath().apply {
////        this.instanceCount = libraryLeak.leakTraces.size
////        this.leakReason = leakTraceObject.leakingStatusReason
////        this.signature = libraryLeak.signature
////        this.gcRoot = gcRoot
////      }
////      mLeakModel.gcPaths.add(leakTraceChainModel)
//
//      // 添加索引到的trace path
//      for (reference in referencePath) {
//        val clazz = reference.originObject.className
//        val referenceName = reference.referenceName
//        val referenceDisplayName = reference.referenceDisplayName
//        val referenceGenericName = reference.referenceGenericName
//        val referenceType = reference.referenceType.toString()
//        val declaredClassName = reference.owningClassName
//
//        Log.i(TAG, "clazz:" + clazz +
//          ", referenceName:" + referenceName
//          + ", referenceDisplayName:" + referenceDisplayName
//          + ", referenceGenericName:" + referenceGenericName
//          + ", referenceType:" + referenceType
//          + ", declaredClassName:" + declaredClassName)
//
////        val leakPathItem = HeapReport.GCPath.PathItem().apply {
////          this.reference = if (referenceDisplayName.startsWith("["))
////            clazz
////          else  //数组类型[]
////            "$clazz.$referenceDisplayName"
////          this.referenceType = referenceType
////          this.declaredClass = declaredClassName
////        }
////        leakTraceChainModel.path.add(leakPathItem)
//      }
//
//      // 添加本身trace path
////      leakTraceChainModel.path.add(HeapReport.GCPath.PathItem().apply {
////        reference = leakTraceObject.className
////        referenceType = leakTraceObject.typeName
////      })
//      break
//    }
//    Log.i(TAG,
//      "=======================================================================")
//
//    val endTime = System.currentTimeMillis()
//
////    mLeakModel.runningInfo!!.findGCPathTime = ((endTime - startTime).toFloat() / 1000).toString()
//
//    Log.i(TAG, "findPathsToGcRoot cost time: "
//      + (endTime - startTime).toFloat() / 1000)
//  }

  private fun printHeapClass(list: List<HeapObject.HeapClass>) {
    val sb = StringBuilder()
    list.forEach {
      sb.append(it.name).append(", ")
    }
    Log.i(TAG, "printHeapClass: $sb")
  }

  private fun printFields(instance: HeapObject.HeapInstance) {
    val sb = StringBuilder()

    instance.readFields().forEach {
      sb.append("${it.name}, ")
    }
    Log.i(TAG, "printFields: $sb")
  }

  private fun updateClassObjectCounterMap(
    classObCountMap: MutableMap<Long, ObjectCounter>,
    instanceClassId: Long,
    isLeak: Boolean
  ): ObjectCounter {
    val objectCounter = classObCountMap[instanceClassId] ?: ObjectCounter().also {
      classObCountMap[instanceClassId] = it
    }

    objectCounter.allCnt++

    if (isLeak) {
      objectCounter.leakCnt++
    }

    return objectCounter
  }

  class ObjectCounter {
    var allCnt = 0
    var leakCnt = 0
  }
}
