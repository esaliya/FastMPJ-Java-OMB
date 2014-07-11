package org.saliya.fmpj.omb.collectives;

import mpi.Intracomm;
import mpi.MPI;
import mpi.MPIException;
import org.saliya.fmpj.util.MpiOps;

public class OsuAllGather {
    public static void main(String[] args) throws MPIException {
        args = MPI.Init(args);

        Intracomm comm = MPI.COMM_WORLD;
        int rank = comm.Rank();
        int numProcs = comm.Size();

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
        byte[] rbuff = new byte[byteBytes*numProcs];

        String msg = "Rank " + rank + " is on " + MPI.Get_processor_name() + "\n";
        msg = MpiOps.allReduceStr(msg, comm);
        if (rank == 0){
            System.out.println(msg);
            System.out.println("#Bytes\tAvgLatency(us)\tMinLatency(us)\tMaxLatency(us)\t#Itr");
        }

        double [] vbuff = new double[1];
        for (int numBytes = 0; numBytes <= maxMsgSize; numBytes = (numBytes == 0 ? 1 : numBytes*2)){
            for (int i = 0; i < byteBytes; ++i){
                sbuff[i] = ((byte)'a');
            }

            if (numBytes > largeMsgSize){
                skip = skipLarge;
                iterations = iterationsLarge;
            }
            comm.Barrier();

            double timer = 0.0;
            double tStart, tStop;
            double minLatency, maxLatency, avgLatency;
            for (int i = 0; i < iterations + skip; ++i){
                tStart = MPI.Wtime();
                comm.Allgather(sbuff,0,numBytes, MPI.BYTE, rbuff, 0, numBytes, MPI.BYTE);
                tStop = MPI.Wtime();
                if (i >= skip){
                    timer += tStop - tStart;
                }
                comm.Barrier();
            }
            double latency = (timer *1e6)/iterations;
            vbuff[0] = latency;
            comm.Reduce(vbuff,0,vbuff,0,1,MPI.DOUBLE,MPI.MIN,0);
            minLatency = vbuff[0];
            vbuff[0] = latency;
            comm.Reduce(vbuff,0,vbuff,0,1,MPI.DOUBLE,MPI.MAX,0);
            maxLatency = vbuff[0];
            vbuff[0] = latency;
            comm.Reduce(vbuff,0,vbuff,0,1,MPI.DOUBLE,MPI.SUM,0);
            avgLatency = vbuff[0] / numProcs;
            if (rank == 0){
                System.out.println(numBytes + "\t" + avgLatency +"\t" + minLatency + "\t" + maxLatency + "\t" + iterations);
            }
            comm.Barrier();
        }
        MPI.Finalize();

    }
}
