package p4nic

import chisel3._
import chisel3.util._
import common.axi._
import common.Delay
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._



class DataChoice extends Module {
    val io = IO(new Bundle {
        // Net data
        val DataIn   = Flipped(Decoupled(UInt(512.W)))
		val IndexIn = Flipped(Decoupled(new Idx()))

		val rx_idx	= Flipped(Decoupled(new Eng_Idx()))

        // User data
		val IndexOut = (Decoupled(new Idx()))
        val DataOut  = (Decoupled(UInt(512.W)))

    })


    Collector.fire(io.DataIn)
    Collector.fire(io.IndexIn)
    Collector.fire(io.rx_idx)
    Collector.fire(io.IndexOut)
    Collector.fire(io.DataOut)


	val data_table1			=  XRam(UInt(512.W),entries=8192,memory_type="ultra",latency=1)
	val data_table2			=  XRam(UInt(512.W),entries=8192,memory_type="ultra",latency=1)
	val data_in_fifo		= XQueue(UInt(512.W),512)
	val data_out_fifo		= XQueue(UInt(512.W),512)
	val index_in_fifo		= XQueue(new Idx(),512)
	val rx_idx_fifo			= XQueue(new Eng_Idx(),512)
	io.DataIn				<> data_in_fifo.io.in
	io.IndexIn				<> index_in_fifo.io.in
	io.rx_idx				<> rx_idx_fifo.io.in
	io.DataOut				<> data_out_fifo.io.out


	val head				= RegInit(VecInit(Seq.fill(CONFIG.ENG_NUM)(0.U(6.W))))
	val tail				= RegInit(VecInit(Seq.fill(CONFIG.ENG_NUM)(0.U(6.W))))
	val next_round			= RegInit(VecInit(Seq.fill(CONFIG.ENG_NUM)(false.B)))

	val regSlice	= Module(new RegSlice(UInt(512.W)))
	val addr1 		= RegInit(0.U(13.W))
	val wr_en1 		= RegInit(0.U(1.W))
	val data1 		= RegInit(0.U(512.W))
	val addr2 		= RegInit(0.U(13.W))
	val wr_en2 		= RegInit(0.U(1.W))
	val data2 		= RegInit(0.U(512.W))



	val sIDLE :: sWait :: sData0 :: sData1 :: Nil = Enum(4)
	val state                   = RegInit(sIDLE)
	val state_r                 = RegInit(sIDLE)
	val engine_idx				= Reg(UInt(7.W))
	val rx_idx					= RegInit(0.U(7.W))

	index_in_fifo.io.out.ready					:= (state === sIDLE) & io.IndexOut.ready
	data_in_fifo.io.out.ready					:= ((state === sData0) & ((head(engine_idx) =/= tail(engine_idx)) || (~next_round(engine_idx))))||(state === sData1)
	rx_idx_fifo.io.out.ready					:= (state_r === sIDLE)

    // ToZero(data_table1.io.addr_a)                 
    ToZero(data_table1.io.addr_b)                 
    // ToZero(data_table1.io.wr_en_a)               
    // ToZero(data_table1.io.data_in_a)   
    // ToZero(data_table2.io.addr_a)                 
    ToZero(data_table2.io.addr_b)                 
    // ToZero(data_table2.io.wr_en_a)               
    // ToZero(data_table2.io.data_in_a)  
	ToZero(wr_en1)
	ToZero(wr_en2)


	ToZero(io.IndexOut.valid)    
	ToZero(io.IndexOut.bits)       
	ToZero(regSlice.io.upStream.valid)    
	ToZero(regSlice.io.upStream.bits) 
	

	data_table1.io.addr_a				:= addr1
	data_table1.io.wr_en_a				:= wr_en1
	data_table1.io.data_in_a			:= data1
	data_table2.io.addr_a				:= addr2
	data_table2.io.wr_en_a				:= wr_en2
	data_table2.io.data_in_a			:= data2


	switch(state){
		is(sIDLE){
			when(index_in_fifo.io.out.fire()){	
				engine_idx				:= index_in_fifo.io.out.bits.engine_idx
				io.IndexOut.valid		:= 1.U
				io.IndexOut.bits		:= index_in_fifo.io.out.bits
				state					:= sData0
			}
		}
		is(sData0){
			when((head(engine_idx) =/= tail(engine_idx)) || (~next_round(engine_idx))){
				when(data_in_fifo.io.out.fire()){
					addr1				:= Cat(engine_idx,head(engine_idx))
					wr_en1				:= 1.U
					data1				:= data_in_fifo.io.out.bits					
					state				:= sData1
				}
			}
		}
		is(sData1){
			when(data_in_fifo.io.out.fire()){
				addr2					:= Cat(engine_idx,head(engine_idx))
				wr_en2					:= 1.U
				data2					:= data_in_fifo.io.out.bits
				when(head(engine_idx) === 63.U){
					head(engine_idx)	:= 0.U
					next_round(engine_idx):= true.B
				}.otherwise{
					head(engine_idx)	:= head(engine_idx) + 1.U
				}
				state					:= sIDLE
			}
		}	
	}


	switch(state_r){
		is(sIDLE){
			when(rx_idx_fifo.io.out.fire()){
				when(rx_idx_fifo.io.out.bits.is_empty){
					state_r					:= sIDLE
				}.otherwise{	
					data_table1.io.addr_b	:= Cat(rx_idx_fifo.io.out.bits.engine_idx,tail(rx_idx_fifo.io.out.bits.engine_idx))
					rx_idx					:= rx_idx_fifo.io.out.bits.engine_idx
					state_r					:= sData0	
				}				
			}.otherwise{
				state_r					:= sIDLE
			}
		}	
		is(sData0){
			when(((head(rx_idx) =/= tail(rx_idx)) || (next_round(rx_idx)))&(regSlice.io.upStream.ready)){	
				regSlice.io.upStream.valid		:= 1.U
				regSlice.io.upStream.bits		:= data_table1.io.data_out_b	
				data_table2.io.addr_b	:= Cat(rx_idx,tail(rx_idx))			
				state_r					:= sData1	
			}.otherwise{
				data_table1.io.addr_b	:= Cat(rx_idx,tail(rx_idx))
				state_r					:= sData0
			}
		}	
		is(sData1){
			when(regSlice.io.upStream.ready){
				regSlice.io.upStream.valid		:= 1.U
				regSlice.io.upStream.bits			:= data_table2.io.data_out_b
				when(tail(rx_idx) === 63.U){
					tail(rx_idx)	:= 0.U
					next_round(rx_idx):= false.B
				}.otherwise{
					tail(rx_idx)	:= tail(rx_idx) + 1.U
				}				
				state_r					:= sIDLE					
			}
		}		
	}


	regSlice.io.downStream			<> data_out_fifo.io.in

    // class ila_choice(seq:Seq[Data]) extends BaseILA(seq)
    // val instIlachoice = Module(new ila_choice(Seq(	
	// 	state,
	// 	state_r,
	// 	rx_idx,
	// 	// rx_idx_fifo.io.out,
	// 	engine_idx,
    // )))
    // instIlachoice.connect(clock) 


}
