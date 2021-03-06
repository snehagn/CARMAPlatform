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

package gov.dot.fhwa.saxton.carma.guidance.plugins;

/**
 * Plugin's primary means of interacting with the PluginManager back in the main Guidance package
 */
public interface PluginManagementService {
    /**
     * Get a tactical plugin from the plugin manager by name, if it exists.
     * 
     * @param pluginName The name of the plugin to query for in string form. Must match the desired plugins componentName value
     * @return The plugin, if it is registered. Returns null otherwise.
     */
    ITacticalPlugin getTacticalPluginByName(String pluginName);
}
