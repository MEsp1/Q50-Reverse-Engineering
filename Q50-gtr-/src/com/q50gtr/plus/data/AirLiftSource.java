package com.q50gtr.plus.data;

/**
 * Marker for a source of AirLift Performance 3H suspension data: corner
 * pressures and tank pressure.
 *
 * The 3H wire protocol is deliberately NOT guessed at here. This interface
 * only fixes the shape of the data so that a verified transport can be dropped
 * in later without the chassis page changing.
 */
public interface AirLiftSource extends DataSource {
}
