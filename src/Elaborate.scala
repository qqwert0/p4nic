package p4nic
import chisel3._
import chisel3.util._
import chisel3.stage.{ChiselGeneratorAnnotation, ChiselStage}
import firrtl.options.TargetDirAnnotation

object elaborate extends App {
	println("Generating a %s class".format(args(0)))
	val stage	= new chisel3.stage.ChiselStage
	val arr		= Array("-X", "sverilog", "--full-stacktrace")
	val dir 	= TargetDirAnnotation("Verilog")

	args(0) match{
		case "WorkerNICTop" => stage.execute(arr,Seq(ChiselGeneratorAnnotation(() => new WorkerNICTop()),dir))
		case "DenseNICTop" => stage.execute(arr,Seq(ChiselGeneratorAnnotation(() => new DenseNICTop()),dir))
		case "P4nicSpase" => stage.execute(arr,Seq(ChiselGeneratorAnnotation(() => new P4nicSpase()),dir))
		case "P4Sim" => stage.execute(arr,Seq(ChiselGeneratorAnnotation(() => new P4Sim()),dir))
		case _ => println("Module match failed!")
	}
}