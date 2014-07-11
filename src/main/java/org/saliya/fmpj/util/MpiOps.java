package org.saliya.fmpj.util;

import mpi.Intracomm;
import mpi.MPI;
import mpi.MPIException;

import java.util.Arrays;
import java.util.stream.IntStream;


public class MpiOps {
    public static String allReduceStr(String value, Intracomm comm) throws MPIException {
        char [] characters = value.toCharArray();
        int [] length = new int[]{characters.length};
        int size = comm.Size();
        int [] lengths = new int[size];
        MPI.COMM_WORLD.Allgather(length, 0, 1, MPI.INT, lengths, 0, 1, MPI.INT);
        int [] displas = new int[size];
        displas[0] = 0;
        System.arraycopy(lengths,0,displas,1, size - 1);
        Arrays.parallelPrefix(displas,(m,n) -> m+n);
        int count = IntStream.of(lengths).sum(); // performs very similar to usual for loop, so no harm done
        char [] recv = new char[count];
        MPI.COMM_WORLD.Allgatherv(characters,0,characters.length,MPI.CHAR,recv,0,lengths,displas,MPI.CHAR);
        return new String(recv);
    }
}
