/*
 *  Copyright 2018 Adobe Systems Incorporated. All rights reserved.
 *  This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License. You may obtain a copy
 *  of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *  OF ANY KIND, either express or implied. See the License for the specific language
 *  governing permissions and limitations under the License.
 */

package com.adobe.aam.shredder.core.trigger;

/**
 * The JSON message will be converted to a TriggerMessage.
 * Implement this interface with you desired trigger (eg. TerminationTrigger) and add the desired JSON fields.
 * Make sure to expose getters for each JSON field, as these are being used by the command macro replacer.
 */
public interface TriggerMessage {
}
