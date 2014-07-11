package org.saliya.fmpj.omb.p2p;

import mpi.Intracomm;
import mpi.MPI;
import mpi.MPIException;
import org.saliya.fmpj.util.MpiOps;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class OsuLaten {
    public static void main(String[] args) throws MPIException {
        args = MPI.Init(args);

        Intracomm comm = MPI.COMM_WORLD;
        int rank = comm.Rank();
        int numProcs = comm.Size();

        if (numProcs != 2){
            System.out.println("This test requires exactly two processes\n");
            MPI.Finalize();
            return;
        }

        int maxMsgSize = 1<<20; // 1MB, i.e. 1024x1024 bytes
        int largeMsgSize = 8192;
        int skip = 200;
        int skipLarge = 10;
        int iterations = 1000;
        int iterationsLarge = 100;

        if (args.length >= 1){
            maxMsgSize = Integer.parseInt(args[0]);
        }

        if (args.length == 2){
            iterations = iterationsLarge = Integer.parseInt(args[1]);
        }

        int byteBytes = maxMsgSize;
        byte[] sbuff = new byte[byteBytes];
        byte[] rbuff = new byte[byteBytes];



        String msg = "Rank " + rank + " is on " + MPI.Get_processor_name() + "\n";
        msg = MpiOps.allReduceStr(msg, comm);
        if (rank == 0){
            System.out.println(msg);
            System.out.println("#Bytes\tLatency(us)");
            System.out.println("Max message size in chars (casted to bytes): " + maxMsgSize + " in bytes:" + maxMsgSize);
        }

        for (int numBytes = 0; numBytes <= maxMsgSize; numBytes = (numBytes == 0 ? 1 : numBytes*2)){
            /*initialize buffers for each run*/
            for (int i = 0; i < byteBytes; ++i){
                sbuff[i] = ((byte)'a');
                rbuff[i] = ((byte)'b');
            }

            if (numBytes > largeMsgSize){
                skip = skipLarge;
                iterations = iterationsLarge;
            }
            comm.Barrier();

            double tStart = 0.0, tStop = 0.0;
            double minLatency, maxLatency, avgLatency;

            if (rank == 0){
                for (int i = 0; i < iterations + skip; ++i){
                    if (i == skip){
                        tStart = MPI.Wtime();
                    }
                    comm.Send(sbuff,0,numBytes,MPI.BYTE,1,1);
                    comm.Recv(rbuff,0,numBytes,MPI.BYTE,1,1);
                }
                tStop = MPI.Wtime();
            } else if (rank == 1){
                for (int i = 0; i < iterations + skip; ++i){
                    comm.Recv(rbuff,0,numBytes,MPI.BYTE,0,1);
                    comm.Send(sbuff,0,numBytes,MPI.BYTE,0,1);
                }
            }


            if (rank == 0){
                double latency = (tStop - tStart)*1e6 /(2.0 * iterations);
                System.out.println(numBytes + "\t" + new BigDecimal(latency).setScale(2, RoundingMode.UP).doubleValue());
            }
        }
        MPI.Finalize();
    }
}
