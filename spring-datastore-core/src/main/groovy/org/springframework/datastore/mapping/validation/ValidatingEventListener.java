/* Copyright (C) 2010 SpringSource
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.springframework.datastore.mapping.validation;

import org.springframework.context.ApplicationEvent;
import org.springframework.datastore.mapping.core.Datastore;
import org.springframework.datastore.mapping.engine.EntityAccess;
import org.springframework.datastore.mapping.engine.event.AbstractPersistenceEvent;
import org.springframework.datastore.mapping.engine.event.AbstractPersistenceEventListener;
import org.springframework.datastore.mapping.engine.event.PreInsertEvent;
import org.springframework.datastore.mapping.engine.event.PreUpdateEvent;
import org.springframework.datastore.mapping.model.PersistentEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * An {@link org.springframework.datastore.mapping.engine.EntityInterceptor} that uses
 * Spring's validation mechanism to evict objects if an error occurs
 *
 * @author Graeme Rocher
 * @since 1.0
 */
public class ValidatingEventListener extends AbstractPersistenceEventListener {

    public static final String ERRORS_ATTRIBUTE = "org.springframework.datastore.ERRORS";
    public static final String SKIP_VALIDATION_ATTRIBUTE = "org.springframework.datastore.SKIP_VALIDATION";

    public ValidatingEventListener(Datastore datastore) {
        super(datastore);
    }

    @Override
    protected void onPersistenceEvent(final AbstractPersistenceEvent event) {
        if (event instanceof PreInsertEvent) {
            beforeInsert(event.getEntity(), event.getEntityAccess());
        }
        else if (event instanceof PreUpdateEvent) {
            beforeUpdate(event.getEntity(), event.getEntityAccess());
        }
    }

    public boolean supportsEventType(Class<? extends ApplicationEvent> eventType) {
        return PreInsertEvent.class.isAssignableFrom(eventType) ||
               PreUpdateEvent.class.isAssignableFrom(eventType);
    }

    public boolean beforeInsert(PersistentEntity entity, EntityAccess e) {
        return doValidate(entity, e.getEntity());
    }

    public boolean beforeUpdate(PersistentEntity entity, EntityAccess e) {
        return doValidate(entity, e.getEntity());
    }

    private boolean doValidate(PersistentEntity entity, Object o) {
        Validator v = datastore.getMappingContext().getEntityValidator(entity);
        if (v == null) {
            return true;
        }

        final Object skipValidation = datastore.getCurrentSession().getAttribute(o, SKIP_VALIDATION_ATTRIBUTE);
        if ((skipValidation instanceof Boolean) && (Boolean) skipValidation) {
            return true;
        }

        BeanPropertyBindingResult result = new BeanPropertyBindingResult(o, o.getClass().getName());
        v.validate(o, result);
        if (result.hasErrors()) {
            onErrors(o, result);
            return false;
        }

        return true;
    }

    /**
     * Sub classes should override to receive error notifications
     *
     * @param object The object being validated
     * @param errors The errors instance
     */
    protected void onErrors(Object object, Errors errors) {
        datastore.getCurrentSession().setAttribute(object, ERRORS_ATTRIBUTE, errors);
    }
}
