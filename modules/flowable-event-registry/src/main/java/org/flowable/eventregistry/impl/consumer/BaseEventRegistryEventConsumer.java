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
package org.flowable.eventregistry.impl.consumer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.eventregistry.api.EventRegistry;
import org.flowable.eventregistry.api.EventRegistryEvent;
import org.flowable.eventregistry.api.EventRegistryEventConsumer;
import org.flowable.eventregistry.api.runtime.EventCorrelationParameterInstance;
import org.flowable.eventregistry.api.runtime.EventInstance;
import org.flowable.eventregistry.impl.EventRegistryEngineConfiguration;
import org.flowable.eventsubscription.api.EventSubscription;
import org.flowable.eventsubscription.api.EventSubscriptionQuery;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public abstract class BaseEventRegistryEventConsumer implements EventRegistryEventConsumer {

    protected AbstractEngineConfiguration engingeConfiguration;
    protected CommandExecutor commandExecutor;

    public BaseEventRegistryEventConsumer(AbstractEngineConfiguration engingeConfiguration) {
        this.engingeConfiguration = engingeConfiguration;
        this.commandExecutor = engingeConfiguration.getCommandExecutor();
    }

    @Override
    public void eventReceived(EventRegistryEvent event) {
        if (event.getEventObject() != null && event.getEventObject() instanceof EventInstance) {
            eventReceived((EventInstance) event.getEventObject());
        } else {
            if (event.getEventObject() == null) {
                throw new FlowableIllegalArgumentException("No event object was passed to the consumer");
            } else {
                throw new FlowableIllegalArgumentException("Unsupported event object type: " + event.getEventObject().getClass());
            }
        }
    }

    protected abstract void eventReceived(EventInstance eventInstance);

    /**
     * Generates all possible correlation keys for the given correlation parameters.
     * The first element in the list will only have used one parameter. The last element in the list has included all parameters.
     */
    protected Collection<CorrelationKey> generateCorrelationKeys(Collection<EventCorrelationParameterInstance> correlationParameterInstances) {

        if (correlationParameterInstances.isEmpty()) {
            return Collections.emptySet();
        }

        List<EventCorrelationParameterInstance> list = new ArrayList<>(correlationParameterInstances);
        Collection<CorrelationKey> correlationKeys = new HashSet<>();
        for (int i = 1; i <= list.size(); i++) {
            for (int j = 0; j <= list.size() - i; j++) {
                List<EventCorrelationParameterInstance> parameterSubList = list.subList(j, j + i);
                String correlationKey = generateCorrelationKey(parameterSubList);
                correlationKeys.add(new CorrelationKey(correlationKey, parameterSubList));
            }
        }

        return correlationKeys;
    }

    protected String generateCorrelationKey(Collection<EventCorrelationParameterInstance> correlationParameterInstances) {
        Map<String, Object> data = new HashMap<>();
        for (EventCorrelationParameterInstance correlationParameterInstance : correlationParameterInstances) {
            data.put(correlationParameterInstance.getDefinitionName(), correlationParameterInstance.getValue());
        }

        return getEventRegistry().generateKey(data);
    }

    protected EventRegistry getEventRegistry() {
        EventRegistryEngineConfiguration eventRegistryEngineConfiguration = (EventRegistryEngineConfiguration) 
                        engingeConfiguration.getEngineConfigurations().get(EngineConfigurationConstants.KEY_EVENT_REGISTRY_CONFIG);
        return eventRegistryEngineConfiguration.getEventRegistry();
    }

    protected CorrelationKey getCorrelationKeyWithAllParameters(Collection<CorrelationKey> correlationKeys) {
        CorrelationKey result = null;
        for (CorrelationKey correlationKey : correlationKeys) {
            if (result == null || (correlationKey.getParameterInstances().size() >= result.getParameterInstances().size()) ) {
                result = correlationKey;
            }
        }
        return result;
    }

    protected List<EventSubscription> findEventSubscriptions(String scopeType, EventInstance eventInstance,  Collection<CorrelationKey> correlationKeys) {
        return commandExecutor.execute(commandContext -> {

            EventSubscriptionQuery eventSubscriptionQuery = createEventSubscriptionQuery()
                .eventType(eventInstance.getEventModel().getKey())
                .scopeType(scopeType);

            if (!correlationKeys.isEmpty()) {

                Set<String> allCorrelationKeyValues = correlationKeys.stream().map(CorrelationKey::getValue).collect(Collectors.toSet());

                eventSubscriptionQuery.or()
                    .withoutConfiguration()
                    .configurations(allCorrelationKeyValues)
                    .endOr();

            } else {
                eventSubscriptionQuery.withoutConfiguration();

            }

            // Note: the tenantId of the model, not the event instance.
            // The event instance tenantId will always be the 'real' tenantId,
            // but the event could have been deployed to the default tenant
            // (which is reflected in the eventModel tenantId).
            String eventModelTenantId = eventInstance.getEventModel().getTenantId();
            if (eventModelTenantId != null && !Objects.equals(AbstractEngineConfiguration.NO_TENANT_ID, eventModelTenantId)) {
                eventSubscriptionQuery.tenantId(eventModelTenantId);
            }

            return eventSubscriptionQuery.list();

        });
    }

    protected abstract EventSubscriptionQuery createEventSubscriptionQuery();

}
