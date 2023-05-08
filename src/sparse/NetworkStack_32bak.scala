// package p4nic

// import chisel3._
// import chisel3.util._
// import common.axi._
// import chisel3.experimental.ChiselEnum
// import common.storage._
// import qdma._
// import common._
// import common.connection.SimpleRouter
// import common.connection.XArbiter


// class NetworkStackSpa extends Module {
//     val io = IO(new Bundle {
//         // Net data
//         val NetTx      = (Decoupled(new AXIS(512)))
//         val NetRx      = Flipped(Decoupled(new AXIS(512)))
//         // User data
//         val DataTx      = Flipped(Decoupled(UInt(512.W)))
//         val DataRx      = (Decoupled(UInt(512.W)))
//         //write cmd
//         val wrMemCmd    = (Decoupled(new PacketRequest))  //addr(64bit),  size(32bit),   callback=0(64bit)

//         //config
//         val rxIdxInitAddr = Input(UInt(64.W))
//         val rxDataInitAddr = Input(UInt(64.W))
//         val idxTotalLen = Input(UInt(32.W)) 
//         val dataTotalLen= Input(UInt(32.W)) 
//         val nodeRank    = Input(UInt(32.W))
//         val engineRand  = Input(UInt(32.W)) 
//         val RxIdxDepth  = Input(UInt(32.W)) 
//         val IdxTransNum = Input(UInt(32.W))
//         val rfinal  		= Output(Bool())
//     })
    

//     //net packet:
//     //Ether(14B) Index(4B) Bitmap(4B) Next_id(4B) Data(128B) = 26B + 128B = 208bit + 1024bit

//     ToZero(io.wrMemCmd.valid)
//     ToZero(io.wrMemCmd.bits)
//     //TX
//     val data_split = Module(new DataSplit())

// 	val LrouterL1			= SimpleRouter(new Idx(), 4)
// 	val LrouterL2			= Seq.fill(4)(SimpleRouter(new Idx(),4))
//     val LrouterL3			= Seq.fill(16)(SimpleRouter(new Idx(),2))
// 	val GrouterL1			= SimpleRouter(new Idx(), 4)
// 	val GrouterL2			= Seq.fill(4)(SimpleRouter(new Idx(),4))
//     val GrouterL3			= Seq.fill(16)(SimpleRouter(new Idx(),2))
//     val arbiterL1			= XArbiter(new Meta(),4)
//     val arbiterL2			= XArbiter(4)(new Meta(),4)
//     val arbiterL3			= XArbiter(16)(new Meta(),2)

//     val gen_header = Seq.fill(32)(Module(new GenHeader()))
//     val tx_pkg_gen = Module(new NetTxPkg())
//     val eth_lshift = Module(new LSHIFT(14,512)) 
//     val tx_add_eth = Module(new NetTxAddEthSpa())

//     //RX
//     val rx_process = Module(new RXProcess())
//     val eth_rshift = Module(new RSHIFT(14,512))
//     val rx_write_data = Module(new RXWriteData())


//     io.rfinal					:= 	rx_write_data.io.rfinal
//     //TX

//     data_split.io.DataIn        <>  io.DataTx
// 	data_split.io.IndexOut      <>  LrouterL1.io.in//gen_header.io.LocalIndex
//     data_split.io.IndexOut.bits.engine_idx(4,3)      <>  LrouterL1.io.idx
//     data_split.io.DataOut       <>  tx_pkg_gen.io.DataTx
//     data_split.io.InxTotalLen   <>  io.idxTotalLen
//     data_split.io.DataTotalLen  <>  io.dataTotalLen
//     data_split.io.IdxTransNum   <>  io.IdxTransNum

// 	for(i<-0 until 4){
// 		LrouterL1.io.out(i)	<> LrouterL2(i).io.in 
// 		LrouterL2(i).io.idx	<> LrouterL2(i).io.in.bits.engine_idx(2,1)
// 		for(j<-0 until 4){
// 			LrouterL2(i).io.out(j)	        <> LrouterL3(i*4+j).io.in
//             LrouterL3(i*4+j).io.idx	        <> LrouterL3(i*4+j).io.in.bits.engine_idx(0)
//             for(m<-0 until 2){
//                 LrouterL3(i*4+j).io.out(m).bits.block_idx	<> gen_header(i*8+j*2+m).io.LocalIndex.bits
//                 LrouterL3(i*4+j).io.out(m).ready	<> gen_header(i*8+j*2+m).io.LocalIndex.ready
//                 LrouterL3(i*4+j).io.out(m).valid	<> gen_header(i*8+j*2+m).io.LocalIndex.valid
//                 gen_header(i*8+j*2+m).io.Bitmap        <>  io.nodeRank
//                 gen_header(i*8+j*2+m).io.EngineRank    <>  (i*8+j*2+m).U
//             }
// 		}
// 	}

// 	for(i<-0 until 4){
// 		GrouterL1.io.out(i)	<> GrouterL2(i).io.in 
// 		GrouterL2(i).io.idx	<> GrouterL2(i).io.in.bits.engine_idx(2,1)
// 		for(j<-0 until 4){
// 			GrouterL2(i).io.out(j)	        <> GrouterL3(i*4+j).io.in
//             GrouterL3(i*4+j).io.idx	        <> GrouterL3(i*4+j).io.in.bits.engine_idx(0)            
//             for(m<-0 until 2){
//                 GrouterL3(i*4+j).io.out(m).bits.block_idx	<> gen_header(i*8+j*2+m).io.GlobeIndex.bits
//                 GrouterL3(i*4+j).io.out(m).ready	<> gen_header(i*8+j*2+m).io.GlobeIndex.ready
//                 GrouterL3(i*4+j).io.out(m).valid	<> gen_header(i*8+j*2+m).io.GlobeIndex.valid
//             }

// 		}
// 	}


// 	for(i<-0 until 4){
// 		arbiterL1.io.in(i)	<> RegSlice(arbiterL2(i).io.out)
// 		for(j<-0 until 4){
// 			arbiterL2(i).io.in(j)	<> RegSlice(arbiterL3(i*4+j).io.out)
//             for(m<-0 until 2){
//                 arbiterL3(i*4+j).io.in(m)	<> RegSlice(gen_header(i*8+j*2+m).io.MetaOut)      
//             }      
// 		}
// 	}


// 	rx_process.io.GlobalIndex     <>  GrouterL1.io.in//gen_header.io.LocalIndex
//     rx_process.io.GlobalIndex.bits.engine_idx(4,3)      <>  GrouterL1.io.idx
//     arbiterL1.io.out       <>  tx_add_eth.io.MetaIn

//     eth_lshift.io.in            <>  tx_pkg_gen.io.NetTx
//     tx_add_eth.io.DataIn        <>  eth_lshift.io.out
//     io.NetTx                    <>  tx_add_eth.io.DataOut


//     //RX
//     rx_process.io.NetRxIn       <>  io.NetRx

//     eth_rshift.io.in            <>  rx_process.io.NetRxOut

//     rx_write_data.io.NetRxIn    <>  eth_rshift.io.out
//     rx_write_data.io.GlobeIndex <>  rx_process.io.GlobeIndex2
//     rx_write_data.io.DataOut    <>  io.DataRx
//     rx_write_data.io.wrMemCmd   <>  io.wrMemCmd
//     rx_write_data.io.rxIdxInitAddr  <>  io.rxIdxInitAddr
//     rx_write_data.io.rxDataInitAddr <>  io.rxDataInitAddr
//     rx_write_data.io.IdxDepth   <>  io.RxIdxDepth


// }