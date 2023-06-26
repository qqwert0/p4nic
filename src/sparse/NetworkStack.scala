package p4nic

import chisel3._
import chisel3.util._
import common.axi._
import chisel3.experimental.ChiselEnum
import common.storage._
import qdma._
import common._
import common.connection.SimpleRouter
import common.connection.XArbiter


class NetworkStackSpa extends Module {
    val io = IO(new Bundle {
        // Net data
        val NetTx      = (Decoupled(new AXIS(512)))
        val NetRx      = Flipped(Decoupled(new AXIS(512)))
        // User data
        val DataTx      = Flipped(Decoupled(UInt(512.W)))
        val DataRx      = (Decoupled(UInt(512.W)))
        //write cmd
        val wrMemCmd    = (Decoupled(new PacketRequest))  //addr(64bit),  size(32bit),   callback=0(64bit)

        //config
        val rxIdxInitAddr = Input(UInt(64.W))
        val rxDataInitAddr = Input(UInt(64.W))
        val idxTotalLen = Input(UInt(32.W)) 
        val dataTotalLen= Input(UInt(32.W)) 
        val nodeRank    = Input(UInt(32.W))
        val engineRand  = Input(UInt(32.W)) 
        val RxIdxDepth  = Input(UInt(32.W)) 
        val IdxTransNum = Input(UInt(32.W))
        val rfinal  		= Output(Bool())
        //token
        val token_small  = Input(UInt(8.W))
        val token_big    = Input(UInt(8.W))
    })
    
    //token 
    // val big_cnt             = RegInit(0.U(8.W))


    // when(big_cnt === io.token_big){
    //     big_cnt             := 0.U
    // }.otherwise{
    //     big_cnt             := big_cnt + 1.U
    // }




    //net packet:
    //Ether(14B) Index(4B) Bitmap(4B) Next_id(4B) Data(128B) = 26B + 128B = 208bit + 1024bit

    ToZero(io.wrMemCmd.valid)
    ToZero(io.wrMemCmd.bits)
    //TX
    val data_split = Module(new DataSplit())

	val LrouterL1			= SimpleRouter(new Idx(), CONFIG.ENG_NUM/64)
	val LrouterL2			= Seq.fill(CONFIG.ENG_NUM/64)(SimpleRouter(new Idx(),4))
    val LrouterL3			= Seq.fill(CONFIG.ENG_NUM/16)(SimpleRouter(new Idx(),4))
    val LrouterL4			= Seq.fill(CONFIG.ENG_NUM/4)(SimpleRouter(new Idx(),4))
	val GrouterL1			= SimpleRouter(new Idx(), CONFIG.ENG_NUM/64)
	val GrouterL2			= Seq.fill(CONFIG.ENG_NUM/64)(SimpleRouter(new Idx(),4))
    val GrouterL3			= Seq.fill(CONFIG.ENG_NUM/16)(SimpleRouter(new Idx(),4))
    val GrouterL4			= Seq.fill(CONFIG.ENG_NUM/4)(SimpleRouter(new Idx(),4))
    val arbiterL1			= XArbiter(new Meta(),CONFIG.ENG_NUM/64)
    val arbiterL2			= XArbiter(CONFIG.ENG_NUM/64)(new Meta(),4)
    val arbiterL3			= XArbiter(CONFIG.ENG_NUM/16)(new Meta(),4)
    val arbiterL4			= XArbiter(CONFIG.ENG_NUM/4)(new Meta(),4)

    val gen_header = Seq.fill(CONFIG.ENG_NUM)(Module(new GenHeader()))
    val tx_pkg_gen = Module(new NetTxPkg())
    val eth_lshift = Module(new LSHIFT(14,512)) 
    val tx_add_eth = Module(new NetTxAddEthSpa())

    //RX
    val rx_process = Module(new RXProcess())
    val eth_rshift = Module(new RSHIFT(14,512))
    val rx_write_data = Module(new RXWriteData())


    
    Collector.fire(LrouterL1.io.in)
    Collector.fire(LrouterL2(1).io.in)
    Collector.fire(LrouterL3(5).io.in)
    Collector.fire(LrouterL4(23).io.in)
    Collector.fire(LrouterL2(2).io.in)
    Collector.fire(LrouterL3(9).io.in)
    Collector.fire(LrouterL4(38).io.in)    
    Collector.fire(gen_header(93).io.LocalIndex)
    Collector.fire(gen_header(155).io.LocalIndex)
    Collector.fire(gen_header(93).io.MetaOut)
    Collector.fire(gen_header(155).io.MetaOut)
    Collector.fire(arbiterL1.io.out)
    Collector.fire(arbiterL2(1).io.out)
    Collector.fire(arbiterL3(5).io.out)
    Collector.fire(arbiterL4(23).io.out)
    Collector.fire(arbiterL2(2).io.out)
    Collector.fire(arbiterL3(9).io.out)
    Collector.fire(arbiterL4(38).io.out) 

    io.rfinal					:= 	rx_write_data.io.rfinal
    //TX

    // data_split.io.DataIn.valid  := Mux(big_cnt > io.token_small, 0.U, io.DataTx.valid)
    // io.DataTx.ready             := Mux(big_cnt > io.token_small, 0.U, data_split.io.DataIn.ready)
    // data_split.io.DataIn.bits   := io.DataTx.bits
    data_split.io.DataIn        <>  io.DataTx
	data_split.io.IndexOut      <>  LrouterL1.io.in//gen_header.io.LocalIndex
    data_split.io.IndexOut.bits.engine_idx(7,6)      <>  LrouterL1.io.idx
    data_split.io.DataOut       <>  tx_pkg_gen.io.DataTx
    data_split.io.InxTotalLen   <>  io.idxTotalLen
    data_split.io.DataTotalLen  <>  io.dataTotalLen
    data_split.io.IdxTransNum   <>  io.IdxTransNum

	for(i<-0 until CONFIG.ENG_NUM/64){
		LrouterL1.io.out(i)	<> LrouterL2(i).io.in 
		LrouterL2(i).io.idx	<> LrouterL2(i).io.in.bits.engine_idx(5,4)
		for(j<-0 until 4){
			LrouterL2(i).io.out(j)	        <> LrouterL3(i*4+j).io.in
            LrouterL3(i*4+j).io.idx	        <> LrouterL3(i*4+j).io.in.bits.engine_idx(3,2)
            for(k<-0 until 4){
                LrouterL3(i*4+j).io.out(k)	        <> LrouterL4(i*16+j*4+k).io.in
                LrouterL4(i*16+j*4+k).io.idx	<> LrouterL4(i*16+j*4+k).io.in.bits.engine_idx(1,0)            
                for(m<-0 until 4){
                    LrouterL4(i*16+j*4+k).io.out(m).bits.block_idx	<> gen_header(i*64+j*16+k*4+m).io.LocalIndex.bits
                    LrouterL4(i*16+j*4+k).io.out(m).ready	<> gen_header(i*64+j*16+k*4+m).io.LocalIndex.ready
                    LrouterL4(i*16+j*4+k).io.out(m).valid	<> gen_header(i*64+j*16+k*4+m).io.LocalIndex.valid
                    gen_header(i*64+j*16+k*4+m).io.Bitmap        <>  io.nodeRank
                    gen_header(i*64+j*16+k*4+m).io.EngineRank    <>  (i*64+j*16+k*4+m).U
                }
            }
		}
	}

	for(i<-0 until CONFIG.ENG_NUM/64){
		GrouterL1.io.out(i)	<> GrouterL2(i).io.in 
		GrouterL2(i).io.idx	<> GrouterL2(i).io.in.bits.engine_idx(5,4)
		for(j<-0 until 4){
			GrouterL2(i).io.out(j)	        <> GrouterL3(i*4+j).io.in
            GrouterL3(i*4+j).io.idx	        <> GrouterL3(i*4+j).io.in.bits.engine_idx(3,2)   
            for(k<-0 until 4){
                GrouterL3(i*4+j).io.out(k)	        <> GrouterL4(i*16+j*4+k).io.in
                GrouterL4(i*16+j*4+k).io.idx	        <> GrouterL4(i*16+j*4+k).io.in.bits.engine_idx(1,0)                      
                for(m<-0 until 4){
                    GrouterL4(i*16+j*4+k).io.out(m).bits.block_idx	<> gen_header(i*64+j*16+k*4+m).io.GlobeIndex.bits
                    GrouterL4(i*16+j*4+k).io.out(m).ready	<> gen_header(i*64+j*16+k*4+m).io.GlobeIndex.ready
                    GrouterL4(i*16+j*4+k).io.out(m).valid	<> gen_header(i*64+j*16+k*4+m).io.GlobeIndex.valid
                }
            }
		}
	}


	for(i<-0 until CONFIG.ENG_NUM/64){
		arbiterL1.io.in(i)	<> RegSlice(arbiterL2(i).io.out)
		for(j<-0 until 4){
			arbiterL2(i).io.in(j)	<> RegSlice(arbiterL3(i*4+j).io.out)
            for(k<-0 until 4){
                arbiterL3(i*4+j).io.in(k)	<> RegSlice(arbiterL4(i*16+j*4+k).io.out)
                for(m<-0 until 4){
                    arbiterL4(i*16+j*4+k).io.in(m)	<> RegSlice(gen_header(i*64+j*16+k*4+m).io.MetaOut)      
                }  
            }    
		}
	}


	rx_process.io.GlobalIndex     <>  GrouterL1.io.in//gen_header.io.LocalIndex
    rx_process.io.GlobalIndex.bits.engine_idx(7,6)      <>  GrouterL1.io.idx
    arbiterL1.io.out       <>  tx_add_eth.io.MetaIn

    eth_lshift.io.in            <>  tx_pkg_gen.io.NetTx
    tx_add_eth.io.DataIn        <>  eth_lshift.io.out
    io.NetTx                    <>  tx_add_eth.io.DataOut


    //RX
    rx_process.io.NetRxIn       <>  io.NetRx

    eth_rshift.io.in            <>  rx_process.io.NetRxOut

    rx_write_data.io.NetRxIn    <>  eth_rshift.io.out
    rx_write_data.io.GlobeIndex <>  rx_process.io.GlobeIndex2
    rx_write_data.io.DataOut    <>  io.DataRx
    rx_write_data.io.wrMemCmd   <>  io.wrMemCmd
    rx_write_data.io.rxIdxInitAddr  <>  io.rxIdxInitAddr
    rx_write_data.io.rxDataInitAddr <>  io.rxDataInitAddr
    rx_write_data.io.IdxDepth   <>  io.RxIdxDepth



}