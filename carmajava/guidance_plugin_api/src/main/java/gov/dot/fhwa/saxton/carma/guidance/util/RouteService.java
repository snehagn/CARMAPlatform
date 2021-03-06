/*
 * Copyright (C) 2018 LEIDOS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package gov.dot.fhwa.saxton.carma.guidance.util;

import gov.dot.fhwa.saxton.carma.route.Route;
import gov.dot.fhwa.saxton.carma.route.RouteSegment;
import java.util.SortedSet;

/**
 * Interface defining the methods needed to work with Route data
 */
public interface RouteService {
  /**
   * Get the route currently being followed by Guidance, null if not yet configured for a route.
   */
  Route getCurrentRoute();

  /**
   * Get the current route segment the vehicle is on, null if not yet on a route.
   */
  RouteSegment getCurrentRouteSegment();

  /**
   * Get the segment of the route that is valid at the input location
   * 
   * @param location The location in question, in terms of linear distance downtrack on the route
   */
  RouteSegment getRouteSegmentAtLocation(double location);

  /**
   * Get the current downtrack distance of the vehicle in m
   */
  double getCurrentDowntrackDistance();

  /**
   * Get the current crosstrack distance of the vehicle in m
   */
  double getCurrentCrosstrackDistance();

  /**
   * Get the current segment downtrack distance of the vehicle in m
   */
  double getCurrentSegmentDowntrack();

  /**
   * Get the current segment index
   */
  int getCurrentSegmentIndex();

  /**
   * Get the set of all speed limits on the route, sorted by location down the route
   */
  SortedSet<SpeedLimit> getSpeedLimitsOnRoute();

  /**
   * Get the set of all desired lanes on the route, sorted by location down the route
   */
  SortedSet<RequiredLane> getRequiredLanesOnRoute();
  
  /**
   * Get the set of all algorithm flags on the route, sorted by location down the route
   */
  SortedSet<AlgorithmFlags> getAlgorithmFlagsOnRoute();

  /**
   * Get the speed limit at the input location, null if the vehicle is not on a route or the location is not on the
   * current route.
   * 
   * @param location The location in question, in terms of linear downtrack distance
   */
  SpeedLimit getSpeedLimitAtLocation(double location);

  /**
   * Get the desired lane at the input location, null if the vehicle is not on a route or the location is not on the
   * current route.
   * 
   * @param location The location in question, in terms of linear downtrack distance
   */
  RequiredLane getRequiredLaneAtLocation(double location);

  /**
   * Get the algorithm flags at the input location, null if the vehicle is not on a route or the location is not on the
   * current route.
   * 
   * @param location The location in question, in terms of linear downtrack distance
   */
  AlgorithmFlags getAlgorithmFlagsAtLocation(double location);

  /**
   * Get the set of all speed limits with locations in the range (start, end]. Empty set if the vehicle is not on a route
   * or there are no limits in the requested range.
   * 
   * @param start The location of the start of the range (exclusive) in linear downtrack distance
   * @param end The location of the end of the range (inclusive) in linear downtrack distance
   */
  SortedSet<SpeedLimit> getSpeedLimitsInRange(double start, double end);

  /**
   * Get the set of all algorithm flags with locations in the range (start, end]. Empty set if the vehicle is not on a route
   * or there are no limits in the requested range.
   * 
   * @param start The location of the start of the range (exclusive) in linear downtrack distance
   * @param end The location of the end of the range (inclusive) in linear downtrack distance
   */
  SortedSet<AlgorithmFlags> getAlgorithmFlagsInRange(double start, double end);

  /**
   * Get the set of all desired lanes with locations in the range (start, end]. Empty set if the vehicle is not on a route
   * or there are no limits in the requested range.
   * 
   * @param start The location of the start of the range (exclusive) in linear downtrack distance
   * @param end The location of the end of the range (inclusive) in linear downtrack distance
   */
  SortedSet<RequiredLane> getRequiredLanesInRange(double start, double end);

  /**
   * Get the earliest window after the start location at which certain algorithm is enabled
   * @param start The location of the start of the range (exclusive) in linear downtrack distance
   * @param end The location of the end of the range (inclusive) in linear downtrack distance
   * @param algorithm The enabled algorithm flag
   * @return the earliest window (startpoint, endpoint]. If not found, return null 
   */
  double[] getAlgorithmEnabledWindowInRange(double start, double end, String algorithm);
  
  /**
   * Return true if there is a specific flag in the range (start, end]
   * @param start The location of the start of the range (exclusive) in linear downtrack distance
   * @param end The location of the end of the range (inclusive) in linear downtrack distance
   * @param algorithm The enabled algorithm flag
   */
  boolean isAlgorithmEnabledInRange(double start, double end, String algorithm);
  
  /**
   * Return true if current route data is available
   * @return the availability of current route data 
   */
  boolean isRouteDataAvailable();
}
