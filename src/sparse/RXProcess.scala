package p4nic

import chisel3._
import chisel3.util._
import common.axi._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._



class RXProcess extends Module {
    val io = IO(new Bundle {
        // Net data
        val NetRxIn   = Flipped(Decoupled(new AXIS(512)))
        //
        val GlobalIndex  = (Decoupled(new Idx()))
		val GlobeIndex2  = (Decoupled(new WR_Idx()))
        val NetRxOut  = (Decoupled(new AXIS(512)))
    })

	val data_fifo = XQueue(new AXIS(512),1024)
	io.NetRxIn			<> data_fifo.io.in

	val sIDLE :: sPAYLOAD :: Nil = Enum(2)
	val state                   = RegInit(sIDLE)	
	val eth_head 				= Wire(new ETHHeader())
	val index					= WireInit(0.U(32.W))

	val index_pre				= Reg(Vec(CONFIG.ENG_NUM,UInt(32.W)))


	data_fifo.io.out.ready					:= ((state === sIDLE)&(io.NetRxOut.ready & io.GlobalIndex.ready & io.GlobeIndex2.ready))||((state === sPAYLOAD)&(io.NetRxOut.ready))
	ToZero(eth_head)
	ToZero(io.GlobalIndex.valid)
	ToZero(io.GlobalIndex.bits)
	ToZero(io.GlobeIndex2.valid)
	ToZero(io.GlobeIndex2.bits)	
	ToZero(io.NetRxOut.valid)
	ToZero(io.NetRxOut.bits)

	Collector.fire(io.GlobalIndex)
	Collector.fire(io.GlobeIndex2)
	Collector.fireLast(io.NetRxOut)


    for (i <- 0 until CONFIG.ENG_NUM) {
		when(reset.asBool){
			index_pre(i)	:= (i+1).U
		}.elsewhen((state===sIDLE)&(data_fifo.io.out.fire())&(index(8,1) === i.U)){
			when(HToN(eth_head.next_idx) === "hffffffff".U){
				index_pre(i)	:= (i+1).U
			}.otherwise{
				index_pre(i)	:= HToN(eth_head.next_idx)
			}
		}.otherwise{
			index_pre(i)	:= index_pre(i)
		}
    }


	switch(state){
		is(sIDLE){
			when(data_fifo.io.out.fire()){
				io.NetRxOut.valid       := 1.U
                io.NetRxOut.bits        <> data_fifo.io.out.bits
                eth_head                := data_fifo.io.out.bits.data(111,0).asTypeOf(eth_head)

                io.GlobalIndex.valid     := 1.U
                io.GlobalIndex.bits.block_idx      := HToN(eth_head.next_idx)
				index      := HToN(eth_head.index) 
				io.GlobalIndex.bits.engine_idx      := Cat(0.U(1.W),index(31,1))
                io.GlobeIndex2.valid     := 1.U
                io.GlobeIndex2.bits.block_idx      := index_pre(index(8,1))	
				when(HToN(eth_head.next_idx) === "hffffffff".U){
					io.GlobeIndex2.bits.is_last		:= true.B
				}.otherwise{
					io.GlobeIndex2.bits.is_last		:= false.B
				}

                when(data_fifo.io.out.bits.last =/= 1.U){
                    state               := sPAYLOAD
                }

			}
		}
		is(sPAYLOAD){
            when(data_fifo.io.out.fire()){
				io.NetRxOut.valid       := 1.U
                io.NetRxOut.bits        <> data_fifo.io.out.bits
                when(data_fifo.io.out.bits.last === 1.U){
                    state               := sIDLE
                }                
            }
			

		}		
	}

    // class ila_rxpro(seq:Seq[Data]) extends BaseILA(seq)
    // val instIlarx = Module(new ila_rxpro(Seq(	
    //     io.GlobalIndex,
	// 	state
    // )))
    // instIlarx.connect(clock)    


}
