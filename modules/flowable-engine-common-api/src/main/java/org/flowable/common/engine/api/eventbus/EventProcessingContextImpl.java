/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.common.engine.api.eventbus;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Joram Barrez
 */
public class EventProcessingContextImpl implements EventProcessingContext {

    protected String channelKey;
    protected String event;
    protected Map<String, Object> processingData;

    public EventProcessingContextImpl(String channelKey, String event) {
        this.channelKey = channelKey;
        this.event = event;
    }

    @Override
    public void addProcessingData(String key, Object data) {
        if (processingData == null) {
            processingData = new HashMap<>();
        }
        processingData.put(key, data);
    }

    @Override
    public String getChannelKey() {
        return channelKey;
    }

    public void setChannelKey(String channelKey) {
        this.channelKey = channelKey;
    }

    @Override
    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    @Override
    public Map<String, Object> getProcessingData() {
        return processingData;
    }

    public void setProcessingData(Map<String, Object> processingData) {
        this.processingData = processingData;
    }

}
