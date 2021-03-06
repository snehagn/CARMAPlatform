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

package gov.dot.fhwa.saxton.carma.route;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import gov.dot.fhwa.saxton.carma.rosutils.SaxtonLogger;
import org.apache.commons.logging.Log;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Loads a route based on the provided file path.
 */
public class FileStrategy implements IRouteLoadStrategy{
  protected String filePath;
  protected SaxtonLogger log;

  /**
   * Constructor initializes a FileStrategy by providing the file path
   * @param path the file path
   */
  public FileStrategy(String path, Log log){
    this.filePath = path;
    this.log = new SaxtonLogger(this.getClass().getSimpleName(), log);
  }

  @Override public Route load() {
    try {
      log.info("Trying to load route: " + filePath);
      FileReader fr = new FileReader(filePath);
      YamlReader reader = new YamlReader(fr);
      Route route = reader.read(gov.dot.fhwa.saxton.carma.route.Route.class);
      RouteValidator validator = new RouteValidator(log.getBaseLoggerObject());
      validator.validateRoute(route);
      if (!route.isValid()) {
        log.warn("An invalid route was loaded from file: " + filePath);
      }
      return route;
    } catch (FileNotFoundException e) {
      log.warn("FileNotFoundException in FileStrategy route load for file: " + filePath, e);
    } catch (YamlException e) {
      log.warn("YamlException in FileStrategy route for file: " + filePath, e);
    } catch (Exception e) {
      log.warn("Exception in FileStrategy route for file: " + filePath, e);
    }
    return null;
  }
}
