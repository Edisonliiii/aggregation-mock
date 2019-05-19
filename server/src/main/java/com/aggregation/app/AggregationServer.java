/**
 * Interface for aggregation server
 * ---- Generate Response
 * @author Jialiang Li
 * @number a1700210
 */
package com.aggregation.app;

import java.io.IOException;

public interface AggregationServer{
	// Fire the aggregation server
    void startServer() throws IOException,InterruptedException;
}