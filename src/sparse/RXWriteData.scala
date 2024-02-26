package p4nic

import chisel3._
import chisel3.util._
import common.axi._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._



class RXWriteData extends Module {
    val io = IO(new Bundle {
        // Net data
        val NetRxIn     = Flipped(Decoupled(new AXIS(512)))
        //
        val GlobeIndex  = Flipped(Decoupled(new WR_Idx()))
        val DataOut     = (Decoupled(UInt(512.W)))

        val wrMemCmd    = (Decoupled(new PacketRequest))  //addr(64bit),  size(32bit),   callback=0(64bit)

        //config
        val rxIdxInitAddr = Input(UInt(64.W))
        val rxDataInitAddr = Input(UInt(64.W))
        val IdxDepth    = Input(UInt(32.W))
		val rfinal  		= Output(Bool())
        val reset_final     = Input(Bool())
      
    })

	val data_fifo = XQueue(new AXIS(512),4096)
    val meta_fifo = XQueue(new WR_Idx(),1024)
	io.NetRxIn			    <> data_fifo.io.in
    io.GlobeIndex			<> meta_fifo.io.in

    val out_fifo = XQueue(UInt(512.W),1024)
    io.DataOut              <> out_fifo.io.out

	val sIDLE :: sINDEX :: sWRITEIDX :: sWRITEDATA :: sPAYLOAD :: Nil = Enum(5)
	val state                   = RegInit(sIDLE)
	val idx_num 				= RegNext(io.IdxDepth)	
	val data_num 				= RegNext(io.IdxDepth<<5.U)
    val shift_cnt               = RegInit(0.U(4.W))	
    val index_cnt               = RegInit(0.U(32.W))
    val data_cnt                = RegInit(0.U(32.W))
    val data_out_cnt            = RegInit(0.U(32.W))
    val index_tmp               = RegInit(0.U(512.W))
    val rxfinal                 = RegInit(Bool(),false.B)
	val rxfinal_cnt             = RegInit(0.U(8.W))

    val idxaddr_offset          = RegInit(0.U(64.W))
    val dataaddr_offset         = RegInit(0.U(64.W))

	io.rfinal					:= 	rxfinal


	meta_fifo.io.out.ready					:= (state === sIDLE)
    data_fifo.io.out.ready					:= (state === sPAYLOAD) & out_fifo.io.in.ready 
	
    val state_reg               = RegInit(0.U(32.W))
    state_reg                   := RegNext(state)
    val rxfinal_cnt_reg         = RegInit(0.U(32.W))
    rxfinal_cnt_reg             := RegNext(rxfinal_cnt)    
    Collector.report(state_reg)
    Collector.report(rxfinal)
    Collector.report(rxfinal_cnt_reg)
	Collector.fire(io.GlobeIndex)
	Collector.fire(io.NetRxIn)
	Collector.fire(io.DataOut)
    ToZero(out_fifo.io.in.valid)
	ToZero(out_fifo.io.in.bits)
	ToZero(io.wrMemCmd.valid)
	ToZero(io.wrMemCmd.bits)


    when((state===sIDLE)&(meta_fifo.io.out.fire())&(meta_fifo.io.out.bits.is_last) & (rxfinal_cnt === (CONFIG.ENG_NUM-1).U)){
        rxfinal             := true.B
    }.elsewhen(RegNext(io.reset_final)){
        rxfinal             := false.B
    }.otherwise{
        rxfinal             := rxfinal
    }



	switch(state){
		is(sIDLE){
			when(meta_fifo.io.out.fire()){
                index_tmp               := Cat(meta_fifo.io.out.bits.block_idx,index_tmp(511,32))
                data_cnt                := data_cnt + 1.U

				when(meta_fifo.io.out.bits.is_last){
					rxfinal_cnt			:= rxfinal_cnt + 1.U
				}


                when((meta_fifo.io.out.bits.is_last) && (rxfinal_cnt === (CONFIG.ENG_NUM-1).U)){
					shift_cnt           := shift_cnt
					rxfinal_cnt			:= 0.U
                    state               := sINDEX 
                }.elsewhen(shift_cnt === 15.U){
                    shift_cnt           := shift_cnt
                    state               := sINDEX 
                }.otherwise{
                    shift_cnt           := shift_cnt + 1.U
                    state               := sIDLE
                }        

			}
		}
		is(sINDEX){
            when(out_fifo.io.in.ready){
				out_fifo.io.in.valid        := 1.U
                out_fifo.io.in.bits         := index_tmp >> ((15.U-shift_cnt)<<5.U)
                index_cnt                   := index_cnt + 1.U
                shift_cnt			        := 0.U
                when((index_cnt === idx_num - 1.U) || rxfinal){
                    state               := sWRITEIDX
                }.otherwise{
                    state               := sIDLE
                }                
            }
		} 
		is(sWRITEIDX){
            when(io.wrMemCmd.ready){
				io.wrMemCmd.valid           := 1.U
                io.wrMemCmd.bits.addr       := io.rxIdxInitAddr + idxaddr_offset
                io.wrMemCmd.bits.size       := index_cnt << 6.U
                io.wrMemCmd.bits.callback   := 0.U
                index_cnt                   := 0.U
                state                       := sWRITEDATA
                when(rxfinal){
                    idxaddr_offset          := 0.U
                }.otherwise{
                    idxaddr_offset          := idxaddr_offset + (index_cnt << 6.U)
                }                
            }
		}
		is(sWRITEDATA){
            when(io.wrMemCmd.ready){
				io.wrMemCmd.valid           := 1.U
                io.wrMemCmd.bits.addr       := io.rxDataInitAddr + dataaddr_offset
                io.wrMemCmd.bits.size       := data_cnt << 7.U
                io.wrMemCmd.bits.callback   := 0.U                
                state                       := sPAYLOAD
                when(rxfinal){
                    dataaddr_offset         := 0.U
                }.otherwise{
                    dataaddr_offset         := dataaddr_offset + (data_cnt << 7.U)
                }                
            }
		}
		is(sPAYLOAD){
            when(data_fifo.io.out.fire()){
				out_fifo.io.in.valid        := 1.U
                out_fifo.io.in.bits         := data_fifo.io.out.bits.data
                data_out_cnt                := data_out_cnt + 1.U
                when(data_out_cnt === ((data_cnt << 1.U) - 1.U)){
                    data_cnt                := 0.U
                    data_out_cnt            := 0.U
                    state                   := sIDLE
                }.otherwise{
                    state                   := sPAYLOAD
                }                
            }
		}		
	}




    // val iodataout = Wire(UInt(64.W))
    // iodataout := io.DataOut.bits(64,0)


    // class ila_wr(seq:Seq[Data]) extends BaseILA(seq)
    // val instIlaWR = Module(new ila_wr(Seq(	
    //     state,
    //     io.DataOut.ready,
    //     io.DataOut.valid,
    //     // iodataout
    // )))
    // instIlaWR.connect(clock)    



}
