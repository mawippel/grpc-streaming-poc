package com.mawippel.server;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;

/**
 * Custom ForkJoinWorkerThread to be used in gRPC server for the bidirectional endpoint.
 * This change is necessary because of the JDK 9+ class loader behavior:
 * "In Java SE 9, threads that are part of the fork/join common pool will always return the system class loader as their thread context class loader."
 * This behavior was causing ClassNotFoundException in {@link GrpcStreamServiceGRPCServer}.
 *
 * @see <a href="https://stackoverflow.com/questions/49113207/completablefuture-forkjoinpool-set-class-loader">Stack Overflow ref</a>
 */
public class ServerForkJoinWorkerThreadFactory extends ForkJoinWorkerThread {

    /**
     * Creates a ForkJoinWorkerThread operating in the given pool.
     *
     * @param pool the pool this thread works in
     * @throws NullPointerException if pool is null
     */
    public ServerForkJoinWorkerThreadFactory(ForkJoinPool pool) {
        super(pool);
        this.setContextClassLoader(Thread.currentThread().getContextClassLoader());
    }
}
