package p4nic

import chisel3._
import chisel3.util._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._

object Util{
    def reverse(data:UInt)={
        val width = data.getWidth
        val res =  WireInit(VecInit(Seq.fill(width)(0.U(1.W))))

        for(i<-0 until width/8){
            for(j<-0 until 8){
                res(i*8+j) := data(width-(i*8)+j-8)
            }
        }
        res.asUInt()
    }
}


class ETHHeader extends Bundle{
    val eth_type 	= UInt(16.W)
	val next_idx 	= UInt(32.W)
	val bitmap 	    = UInt(32.W)
	val index 		= UInt(32.W)	
}


class Meta extends Bundle{
	val meta 	= new Header()
    val eng_Idx = new Eng_Idx()
}

class Header extends Bundle{
	val head 	= new ETHHeader()
    val is_empty    = Bool()
}


class Idx extends Bundle{
	val block_idx 	= UInt(32.W)
    val engine_idx  = UInt(8.W)
}


class WR_Idx extends Bundle{
	val block_idx 	= UInt(32.W)
    val is_last     = Bool()
}

class Eng_Idx extends Bundle{
	val engine_idx 	= UInt(7.W)
    val is_empty    = Bool()
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
    def ENG_NUM = 128
}