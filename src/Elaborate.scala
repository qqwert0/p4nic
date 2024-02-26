package p4nic
import chisel3._
import chisel3.util._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage, ChiselOutputFileAnnotation}
import firrtl.options.{TargetDirAnnotation, OutputAnnotationFileAnnotation}
import firrtl.stage.OutputFileAnnotation
import firrtl.options.TargetDirAnnotation

object elaborate extends App {
	println("Generating a %s class".format(args(0)))
	val stage	= new chisel3.stage.ChiselStage
	val arr		= Array("-X", "sverilog", "--full-stacktrace")
	val dir 	= TargetDirAnnotation("Verilog")

	args(0) match{
		case "WorkerNICTop" => stage.execute(arr,Seq(ChiselGeneratorAnnotation(() => new WorkerNICTop()),dir, OutputFileAnnotation(args(0)), OutputAnnotationFileAnnotation(args(0)), ChiselOutputFileAnnotation(args(0))))
		case "WorkerNICTopnew" => stage.execute(arr,Seq(ChiselGeneratorAnnotation(() => new WorkerNICTopnew()),dir))
		case "DenseNICTop" => stage.execute(arr,Seq(ChiselGeneratorAnnotation(() => new DenseNICTop()),dir))
		case "P4nicSpase" => stage.execute(arr,Seq(ChiselGeneratorAnnotation(() => new P4nicSpase()),dir))
		case "P4Sim" => stage.execute(arr,Seq(ChiselGeneratorAnnotation(() => new P4Sim()),dir))
		case _ => println("Module match failed!")
	}
}