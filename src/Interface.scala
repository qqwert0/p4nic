package p4nic

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._

class ETHHeader extends Bundle{
    val eth_type 	= UInt(16.W)
	val next_idx 	= UInt(32.W)
	val bitmap 	    = UInt(32.W)
	val index 		= UInt(32.W)	
}


class Meta extends Bundle{
	val head 	= new ETHHeader()
    val is_empty    = Bool()
}


class Idx extends Bundle{
	val block_idx 	= UInt(32.W)
    val engine_idx  = UInt(8.W)
}

class INDEX_STATE extends Bundle{
	val local_idx 	= UInt(32.W)
    val global_idx  = UInt(32.W)
    val slot_idx    = UInt(1.W)
}

object HToN {
    def apply(in: UInt) = {
        assert(in.getWidth % 8 == 0)
        val segNum = in.getWidth / 8
        val outSeg = Wire(Vec(segNum, UInt(8.W)))

        for (i <- 0 until segNum) {
            outSeg(segNum-1-i)  := in(8*i+7, 8*i)
        }

        outSeg.asUInt
    }
}
object CONFIG{
    def ENG_NUM = 256
}