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

package gov.dot.fhwa.saxton.carma.guidance.trajectory;

import gov.dot.fhwa.saxton.carma.guidance.maneuvers.IComplexManeuver;
import gov.dot.fhwa.saxton.carma.guidance.maneuvers.IManeuver;
import gov.dot.fhwa.saxton.carma.guidance.maneuvers.ISimpleManeuver;
import gov.dot.fhwa.saxton.carma.guidance.maneuvers.LateralManeuver;
import gov.dot.fhwa.saxton.carma.guidance.maneuvers.LongitudinalManeuver;
import gov.dot.fhwa.saxton.carma.guidance.maneuvers.ManeuverType;
import gov.dot.fhwa.saxton.carma.guidance.util.intervaltree.Interval;
import gov.dot.fhwa.saxton.carma.guidance.util.intervaltree.IntervalTree;
import gov.dot.fhwa.saxton.carma.guidance.util.intervaltree.IntervalTreeFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

/**
 * Data structure and helper method container for describing and creating vehicle trajectories
 * </p>
 * Stores both lateral and longitudinal trajectories made of maneuvers that can be individually executed
 * to command the vehicle
 */
public class Trajectory {

  protected double startLocation;
  protected double endLocation;
  protected IntervalTree<LateralManeuver> lateralManeuvers;
  protected IntervalTree<LongitudinalManeuver> longitudinalManeuvers;
  protected IComplexManeuver complexManeuver = null;
  protected static final double DISTANCE_EPSILON = 0.00001;

  /**
   * Create a new trajectory instance that will command the vehicle on distances [startLocation, endLocation)
   */
  public Trajectory(double startLocation, double endLocation) {
    this.startLocation = startLocation;
    this.endLocation = endLocation;

    lateralManeuvers = IntervalTreeFactory.buildIntervalTree();
    longitudinalManeuvers = IntervalTreeFactory.buildIntervalTree();
  }

  /**
   * Deep copy constructor for Trajectory instances
   */
  public Trajectory(Trajectory traj) {
    startLocation = traj.startLocation;
    endLocation = traj.endLocation;

    lateralManeuvers = IntervalTreeFactory.buildIntervalTree();
    longitudinalManeuvers = IntervalTreeFactory.buildIntervalTree();

    for (LateralManeuver m : traj.lateralManeuvers.toSortedList()) {
      lateralManeuvers.insert(new Interval<LateralManeuver>(m, m.getStartDistance(), m.getEndDistance()));
    }
    for (LongitudinalManeuver m : traj.longitudinalManeuvers.toSortedList()) {
      longitudinalManeuvers.insert(new Interval<LongitudinalManeuver>(m, m.getStartDistance(), m.getEndDistance()));
    }

    complexManeuver = traj.complexManeuver;
  }

  /**
   * Get the location along the route that this Trajectory will start at
   */
  public double getStartLocation() {
    return this.startLocation;
  }

  /**
   * Get the location along the route that this Trajectory will finish at
   */
  public double getEndLocation() {
    return this.endLocation;
  }

  /**
   * Add a maneuver to the Trajectory.
   * </p>
   * The maneuver will be added to the appropriate maneuvers list if it fits spatially within the domain of
   * this trajectory instance.
   */
  public boolean addManeuver(ISimpleManeuver maneuver) {
    if (maneuver.getStartDistance() >= startLocation && maneuver.getEndDistance() <= endLocation) {
      if (maneuver instanceof LongitudinalManeuver) {
        return longitudinalManeuvers.insert(new Interval<LongitudinalManeuver>((LongitudinalManeuver) maneuver,
            maneuver.getStartDistance(), maneuver.getEndDistance()));
      } else if (maneuver instanceof LateralManeuver) {
        return lateralManeuvers.insert(new Interval<LateralManeuver>((LateralManeuver) maneuver,
            maneuver.getStartDistance(), maneuver.getEndDistance()));
      } else {
        // Maneuver is neither lateral nor longitudinal, we can't handle this case so reject it
        return false;
      }
    } else {
      return false;
    }
  }

  /**
   * Set the complex maneuver for the current trajectory.
   * <p>
   * In order to accept a complex maneuver, there must not already be a complex maneuver for this trajectory,
   * the requested complex maneuver must be within trajectory boundaries, and the requested complex maneuver
   * must be the last maneuver in the trajectory. If no maneuvers are presently planned after the start of
   * the complex maneuver, the end of this trajectory will be set to the end of the complex manauever.
   * 
   * @param maneuver The complex maneuver to to add
   * @return True if the maneuver has been accepted, false o.w.
   */
  public boolean setComplexManeuver(IComplexManeuver maneuver) {
    if (maneuver == null) {
      return false;
    }

    if (complexManeuver != null) {
      // Only one complex maneuver is allowed per trajectory
      return false;
    }

    if (!(maneuver.getStartDistance() >= startLocation && maneuver.getEndDistance() <= endLocation)) {
      // Must be within bounds like normal maneuvers
      return false;
    }

    if (getNextManeuverAfter(maneuver.getStartDistance(), ManeuverType.LONGITUDINAL) != null
        || getNextManeuverAfter(maneuver.getStartDistance(), ManeuverType.LATERAL) != null) {
      // Complex maneuver must be last maneuver in trajectory
      return false;
    }

    // Check for overlap with any other maneuvers currentl planned
    if (!lateralManeuvers.findIntersectionsWith(new Interval<>(maneuver.getStartDistance(), maneuver.getEndDistance()))
        .isEmpty()) {
      return false;
    }

    if (!longitudinalManeuvers
        .findIntersectionsWith(new Interval<>(maneuver.getStartDistance(), maneuver.getEndDistance())).isEmpty()) {
      return false;
    }

    // Valid complex maneuver received, adjust and accept
    endLocation = maneuver.getEndDistance();
    complexManeuver = maneuver;
    return true;
  }

  /**
   * Get the complex maneuver contained in this trajectory, if it exists.
   * 
   * @return The planned {@link IComplexManeuver} instance if it exists, null o.w.
   */
  public IComplexManeuver getComplexManeuver() {
    return complexManeuver;
  }

  /**
   * Find the earliest available space in the longitudinal domain of the current trajectory for 
   * which a maneuver of the specified size might fit.
   * 
   * @returns The distance location of the start of the window if found, -1 otherwise
   */
  public double findEarliestLongitudinalWindowOfSize(double size) {
    List<IManeuver> maneuvers = new ArrayList<>();
    maneuvers.addAll(longitudinalManeuvers.toSortedList());
    if (complexManeuver != null) {
      maneuvers.add(complexManeuver);
    }

    if (maneuvers.isEmpty()) {
      return getStartLocation();
    }

    double lastEnd = startLocation;
    for (IManeuver m : maneuvers) {
      if (m.getStartDistance() - lastEnd >= size) {
        return lastEnd;
      }

      lastEnd = m.getEndDistance();
    }

    if (lastEnd < endLocation && (endLocation - lastEnd) >= size) {
      return lastEnd;
    }

    return -1;
  }

  /**
   * Find the earliest available space in the lateral domain of the current trajectory for 
   * which a maneuver of the specified size might fit.
   * 
   * @returns The distance location of the start of the window if found, -1 otherwise
   */
  public double findEarliestLateralWindowOfSize(double size) {
    List<IManeuver> maneuvers = new ArrayList<>();
    maneuvers.addAll(lateralManeuvers.toSortedList());
    if (complexManeuver != null) {
      maneuvers.add(complexManeuver);
    }

    if (maneuvers.isEmpty()) {
      return getStartLocation();
    }

    double lastEnd = startLocation;
    for (IManeuver m : maneuvers) {
      if (m.getStartDistance() - lastEnd >= size) {
        return lastEnd;
      }

      lastEnd = m.getEndDistance();
    }

    if (lastEnd < endLocation && (endLocation - lastEnd) >= size) {
      return lastEnd;
    }

    return -1;
  }

  /**
   * Find the latest available space in the longitudinal domain of the current trajectory for 
   * which a maneuver of the specified size might fit.
   * 
   * @returns The distance location of the start of the window if found, -1 otherwise
   */
  public double findLatestLongitudinalWindowOfSize(double size) {
    List<IManeuver> maneuvers = new ArrayList<>();
    maneuvers.addAll(longitudinalManeuvers.toSortedList());
    if (complexManeuver != null) {
      maneuvers.add(complexManeuver);
    }

    if (maneuvers.size() == 0) {
      return endLocation - size;
    }

    double lastStart = endLocation;
    for (int i = maneuvers.size() - 1; i >= 0; i--) {
      IManeuver m = maneuvers.get(i);
      if (lastStart - m.getEndDistance() >= size) {
        return m.getEndDistance();
      }

      lastStart = m.getStartDistance();
    }

    if (lastStart > startLocation && (lastStart - startLocation) > size) {
      return lastStart - size;
    }

    return -1;
  }

  /**
   * Find the latest available space in the lateral domain of the current trajectory for 
   * which a maneuver of the specified size might fit.
   * 
   * @returns The distance location of the start of the window if found, -1 otherwise
   */
  public double findLatestLateralWindowOfSize(double size) {
    List<IManeuver> maneuvers = new ArrayList<>();
    maneuvers.addAll(lateralManeuvers.toSortedList());
    if (complexManeuver != null) {
      maneuvers.add(complexManeuver);
    }

    if (maneuvers.size() == 0) {
      return endLocation - size;
    }

    double lastStart = endLocation;
    for (int i = maneuvers.size() - 1; i >= 0; i--) {
      IManeuver m = maneuvers.get(i);
      if (lastStart - m.getEndDistance() >= size) {
        return m.getEndDistance();
      }

      lastStart = m.getStartDistance();
    }

    if (lastStart > startLocation && (lastStart - startLocation) > size) {
      return lastStart - size;
    }

    return -1;
  }

  /**
   * Get a list of all maneuvers that will be active at loc
   */
  public List<IManeuver> getManeuversAt(double loc) {
    List<IManeuver> out = new ArrayList<>();

    for (Interval<LongitudinalManeuver> mvr : longitudinalManeuvers.findIntersectionsWith(loc)) {
      out.add(mvr.getData());
    }
    for (Interval<LateralManeuver> mvr : lateralManeuvers.findIntersectionsWith(loc)) {
      out.add(mvr.getData());
    }

    if (complexManeuver != null && loc >= complexManeuver.getStartDistance()
        && loc < complexManeuver.getEndDistance()) {
      out.add(complexManeuver);
    }

    return out;
  }

  /**
   * Get a list of all maneuver of a specific type that will be active at loc
   * Undefined behavior if there are overlapping maneuvers of the same type
   */
  public IManeuver getManeuverAt(double loc, ManeuverType type) {
    if (type == ManeuverType.LATERAL) {
      SortedSet<Interval<LateralManeuver>> mvrs = lateralManeuvers.findIntersectionsWith(loc);
      return (mvrs.isEmpty() ? null : mvrs.first().getData());
    }

    if (type == ManeuverType.LONGITUDINAL) {
      SortedSet<Interval<LongitudinalManeuver>> mvrs = longitudinalManeuvers.findIntersectionsWith(loc);
      return (mvrs.isEmpty() ? null : mvrs.first().getData());
    }

    if (type == ManeuverType.COMPLEX) {
      if (complexManeuver != null) {
        if (complexManeuver.getStartDistance() <= loc && complexManeuver.getEndDistance() > loc) {
          return complexManeuver;
        }
      }
    }

    return null;
  }

  /**
   * Get the next maneuver of the specified type which will be wholly after loc, null if one cannot be found
   */
  public IManeuver getNextManeuverAfter(double loc, ManeuverType type) {
    if (type == ManeuverType.LONGITUDINAL) {
      SortedSet<Interval<LongitudinalManeuver>> mvrsAtPt = longitudinalManeuvers.findIntersectionsWith(loc);
      if (!mvrsAtPt.isEmpty()) {
        double endOfMvrAtPt = mvrsAtPt.first().getEnd();
        SortedSet<Interval<LongitudinalManeuver>> mvrs = longitudinalManeuvers
            .findIntersectionsWith(new Interval<>(endOfMvrAtPt, endLocation));
        return (mvrs.isEmpty() ? null : mvrs.first().getData());
      } else {
        SortedSet<Interval<LongitudinalManeuver>> intersections = longitudinalManeuvers.findIntersectionsWith(new Interval<>(loc, endLocation));
        if (!intersections.isEmpty()) {
          return intersections.first().getData();
        } else {
          return null;
        }
      }
    }

    if (type == ManeuverType.LATERAL) {
      SortedSet<Interval<LateralManeuver>> mvrsAtPt = lateralManeuvers.findIntersectionsWith(loc);
      if (!mvrsAtPt.isEmpty()) {
        double endOfMvrAtPt = mvrsAtPt.first().getEnd();
        SortedSet<Interval<LateralManeuver>> mvrs = lateralManeuvers
            .findIntersectionsWith(new Interval<>(endOfMvrAtPt, endLocation));
        return (mvrs.isEmpty() ? null : mvrs.first().getData());
      } else {
        SortedSet<Interval<LateralManeuver>> intersections = lateralManeuvers.findIntersectionsWith(new Interval<>(loc, endLocation));
        if (!intersections.isEmpty()) {
          return intersections.first().getData();
        } else {
          return null;
        }
      }
    }

    if (type == ManeuverType.COMPLEX) {
      if (complexManeuver != null) {
        if (loc < complexManeuver.getStartDistance()) {
          return complexManeuver;
        }
      }

      return null;
    }

    return null;
  }

  /**
   * Get the trajectory's stored lateral maneuvers in sorted order by start location
   */
  public List<LateralManeuver> getLateralManeuvers() {
    List<LateralManeuver> out = new ArrayList<>();
    out.addAll(lateralManeuvers.toSortedList());

    return out;
  }

  /**
   * Get the trajectory's stored longitudinal maneuvers in sorted order by start location
   */
  public List<LongitudinalManeuver> getLongitudinalManeuvers() {
    return longitudinalManeuvers.toSortedList();
  }

  /**
   * Copy all maneuvers (of any type) between the specified distances into the current trajectory
   * 
   * @param src The trajectory to look at for maneuvers to copy
   * @param startDowntrack The start location for the maneuvers to be copied. All maneuvers that start after 
   * this location downtrack will be added.
   * @param endDowntrack The end location for the maneuvers to the copied, maneuvers that end after this location
   * will be ignored in the copying process.
   * @return A boolean value indicating the success or failure of this operation, if false no modification occurs
   */
  public boolean copyManeuvers(Trajectory src, double startDowntrack, double endDowntrack) {
    Trajectory tmp = new Trajectory(this); // Temporary copy to ensure that failed operations don't destroy the trajectory
    
    boolean success = true;
    List<IManeuver> toBeCopied = new ArrayList<>();
    for (IManeuver mvr : src.getManeuvers()) {
      if (mvr.getStartDistance() >= startDowntrack && mvr.getEndDistance() <= endDowntrack) {
        toBeCopied.add(mvr);
        if (mvr instanceof ISimpleManeuver) {
          success = tmp.addManeuver((ISimpleManeuver) mvr);
        } else {
          success = tmp.setComplexManeuver((IComplexManeuver) mvr);
        }

        if (!success) {
          return false;
        }
      }
    }

    // Successfully copied all maneuvers into the temporary copy, proceed to execute for real
    for (IManeuver mvr : toBeCopied) {
      if (mvr instanceof ISimpleManeuver) {
        this.addManeuver((ISimpleManeuver) mvr);
      } else {
        this.setComplexManeuver((IComplexManeuver) mvr);
      }
    }

    return true;
  }

  /**
   * Get the trajectory's stored maneuvers in sorted order by start location
   * <p>
   * Note: This operation is more expensive than it might seem, it requires traversal
   * of two separate trees and a merge of the resulting flattened lists. If possible,
   * call once and cache the result.
   */
  public List<IManeuver> getManeuvers() {
    List<IManeuver> out = new ArrayList<>();

    // Merge the two sorted lists, a la mergesort
    List<LateralManeuver> laterals = lateralManeuvers.toSortedList();
    List<LongitudinalManeuver> longitudinals = longitudinalManeuvers.toSortedList();
    int numElems = laterals.size() + longitudinals.size();

    // Merge the lists by peeling elements off their fronts until one is empty
    while (!laterals.isEmpty() && !longitudinals.isEmpty()) {
        LateralManeuver lat = laterals.get(0);
        LongitudinalManeuver lon = longitudinals.get(0);
        if (lon.getStartDistance() <= lat.getStartDistance()) {
          out.add(laterals.remove(0));
        } else {
          out.add(laterals.remove(0));
        }
    }

    if (laterals.isEmpty()) {
      out.addAll(longitudinals);
    } else if (longitudinals.isEmpty()) {
      out.addAll(laterals);
    }

    if (complexManeuver != null) {
      out.add(complexManeuver);
    }

    return out;
  }

  @Override
  public String toString() {
    return "Trajectory [startLocation=" + startLocation + ", endLocation=" + endLocation + "]";
  }

}
