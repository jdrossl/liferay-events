/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.rivetlogic.event.service.base;

import com.liferay.portal.kernel.bean.BeanReference;
import com.liferay.portal.kernel.bean.IdentifiableBean;
import com.liferay.portal.kernel.dao.jdbc.SqlUpdate;
import com.liferay.portal.kernel.dao.jdbc.SqlUpdateFactoryUtil;
import com.liferay.portal.kernel.dao.orm.DynamicQuery;
import com.liferay.portal.kernel.dao.orm.DynamicQueryFactoryUtil;
import com.liferay.portal.kernel.dao.orm.Projection;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.search.Indexable;
import com.liferay.portal.kernel.search.IndexableType;
import com.liferay.portal.kernel.util.OrderByComparator;
import com.liferay.portal.model.PersistedModel;
import com.liferay.portal.service.BaseLocalServiceImpl;
import com.liferay.portal.service.PersistedModelLocalServiceRegistryUtil;
import com.liferay.portal.service.persistence.UserPersistence;

import com.rivetlogic.event.model.Participant;
import com.rivetlogic.event.service.ParticipantLocalService;
import com.rivetlogic.event.service.persistence.EventPersistence;
import com.rivetlogic.event.service.persistence.ParticipantPersistence;
import com.rivetlogic.event.service.persistence.TokenPersistence;

import java.io.Serializable;

import java.util.List;

import javax.sql.DataSource;

/**
 * Provides the base implementation for the participant local service.
 *
 * <p>
 * This implementation exists only as a container for the default service methods generated by ServiceBuilder. All custom service methods should be put in {@link com.rivetlogic.event.service.impl.ParticipantLocalServiceImpl}.
 * </p>
 *
 * @author juancarrillo
 * @see com.rivetlogic.event.service.impl.ParticipantLocalServiceImpl
 * @see com.rivetlogic.event.service.ParticipantLocalServiceUtil
 * @generated
 */
public abstract class ParticipantLocalServiceBaseImpl
	extends BaseLocalServiceImpl implements ParticipantLocalService,
		IdentifiableBean {
	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify or reference this class directly. Always use {@link com.rivetlogic.event.service.ParticipantLocalServiceUtil} to access the participant local service.
	 */

	/**
	 * Adds the participant to the database. Also notifies the appropriate model listeners.
	 *
	 * @param participant the participant
	 * @return the participant that was added
	 * @throws SystemException if a system exception occurred
	 */
	@Indexable(type = IndexableType.REINDEX)
	@Override
	public Participant addParticipant(Participant participant)
		throws SystemException {
		participant.setNew(true);

		return participantPersistence.update(participant);
	}

	/**
	 * Creates a new participant with the primary key. Does not add the participant to the database.
	 *
	 * @param participantId the primary key for the new participant
	 * @return the new participant
	 */
	@Override
	public Participant createParticipant(long participantId) {
		return participantPersistence.create(participantId);
	}

	/**
	 * Deletes the participant with the primary key from the database. Also notifies the appropriate model listeners.
	 *
	 * @param participantId the primary key of the participant
	 * @return the participant that was removed
	 * @throws PortalException if a participant with the primary key could not be found
	 * @throws SystemException if a system exception occurred
	 */
	@Indexable(type = IndexableType.DELETE)
	@Override
	public Participant deleteParticipant(long participantId)
		throws PortalException, SystemException {
		return participantPersistence.remove(participantId);
	}

	/**
	 * Deletes the participant from the database. Also notifies the appropriate model listeners.
	 *
	 * @param participant the participant
	 * @return the participant that was removed
	 * @throws SystemException if a system exception occurred
	 */
	@Indexable(type = IndexableType.DELETE)
	@Override
	public Participant deleteParticipant(Participant participant)
		throws SystemException {
		return participantPersistence.remove(participant);
	}

	@Override
	public DynamicQuery dynamicQuery() {
		Class<?> clazz = getClass();

		return DynamicQueryFactoryUtil.forClass(Participant.class,
			clazz.getClassLoader());
	}

	/**
	 * Performs a dynamic query on the database and returns the matching rows.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the matching rows
	 * @throws SystemException if a system exception occurred
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public List dynamicQuery(DynamicQuery dynamicQuery)
		throws SystemException {
		return participantPersistence.findWithDynamicQuery(dynamicQuery);
	}

	/**
	 * Performs a dynamic query on the database and returns a range of the matching rows.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to {@link com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS} will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent and pagination is required (<code>start</code> and <code>end</code> are not {@link com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS}), then the query will include the default ORDER BY logic from {@link com.rivetlogic.event.model.impl.ParticipantModelImpl}. If both <code>orderByComparator</code> and pagination are absent, for performance reasons, the query will not have an ORDER BY clause and the returned result set will be sorted on by the primary key in an ascending order.
	 * </p>
	 *
	 * @param dynamicQuery the dynamic query
	 * @param start the lower bound of the range of model instances
	 * @param end the upper bound of the range of model instances (not inclusive)
	 * @return the range of matching rows
	 * @throws SystemException if a system exception occurred
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public List dynamicQuery(DynamicQuery dynamicQuery, int start, int end)
		throws SystemException {
		return participantPersistence.findWithDynamicQuery(dynamicQuery, start,
			end);
	}

	/**
	 * Performs a dynamic query on the database and returns an ordered range of the matching rows.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to {@link com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS} will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent and pagination is required (<code>start</code> and <code>end</code> are not {@link com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS}), then the query will include the default ORDER BY logic from {@link com.rivetlogic.event.model.impl.ParticipantModelImpl}. If both <code>orderByComparator</code> and pagination are absent, for performance reasons, the query will not have an ORDER BY clause and the returned result set will be sorted on by the primary key in an ascending order.
	 * </p>
	 *
	 * @param dynamicQuery the dynamic query
	 * @param start the lower bound of the range of model instances
	 * @param end the upper bound of the range of model instances (not inclusive)
	 * @param orderByComparator the comparator to order the results by (optionally <code>null</code>)
	 * @return the ordered range of matching rows
	 * @throws SystemException if a system exception occurred
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public List dynamicQuery(DynamicQuery dynamicQuery, int start, int end,
		OrderByComparator orderByComparator) throws SystemException {
		return participantPersistence.findWithDynamicQuery(dynamicQuery, start,
			end, orderByComparator);
	}

	/**
	 * Returns the number of rows that match the dynamic query.
	 *
	 * @param dynamicQuery the dynamic query
	 * @return the number of rows that match the dynamic query
	 * @throws SystemException if a system exception occurred
	 */
	@Override
	public long dynamicQueryCount(DynamicQuery dynamicQuery)
		throws SystemException {
		return participantPersistence.countWithDynamicQuery(dynamicQuery);
	}

	/**
	 * Returns the number of rows that match the dynamic query.
	 *
	 * @param dynamicQuery the dynamic query
	 * @param projection the projection to apply to the query
	 * @return the number of rows that match the dynamic query
	 * @throws SystemException if a system exception occurred
	 */
	@Override
	public long dynamicQueryCount(DynamicQuery dynamicQuery,
		Projection projection) throws SystemException {
		return participantPersistence.countWithDynamicQuery(dynamicQuery,
			projection);
	}

	@Override
	public Participant fetchParticipant(long participantId)
		throws SystemException {
		return participantPersistence.fetchByPrimaryKey(participantId);
	}

	/**
	 * Returns the participant with the matching UUID and company.
	 *
	 * @param uuid the participant's UUID
	 * @param  companyId the primary key of the company
	 * @return the matching participant, or <code>null</code> if a matching participant could not be found
	 * @throws SystemException if a system exception occurred
	 */
	@Override
	public Participant fetchParticipantByUuidAndCompanyId(String uuid,
		long companyId) throws SystemException {
		return participantPersistence.fetchByUuid_C_First(uuid, companyId, null);
	}

	/**
	 * Returns the participant matching the UUID and group.
	 *
	 * @param uuid the participant's UUID
	 * @param groupId the primary key of the group
	 * @return the matching participant, or <code>null</code> if a matching participant could not be found
	 * @throws SystemException if a system exception occurred
	 */
	@Override
	public Participant fetchParticipantByUuidAndGroupId(String uuid,
		long groupId) throws SystemException {
		return participantPersistence.fetchByUUID_G(uuid, groupId);
	}

	/**
	 * Returns the participant with the primary key.
	 *
	 * @param participantId the primary key of the participant
	 * @return the participant
	 * @throws PortalException if a participant with the primary key could not be found
	 * @throws SystemException if a system exception occurred
	 */
	@Override
	public Participant getParticipant(long participantId)
		throws PortalException, SystemException {
		return participantPersistence.findByPrimaryKey(participantId);
	}

	@Override
	public PersistedModel getPersistedModel(Serializable primaryKeyObj)
		throws PortalException, SystemException {
		return participantPersistence.findByPrimaryKey(primaryKeyObj);
	}

	/**
	 * Returns the participant with the matching UUID and company.
	 *
	 * @param uuid the participant's UUID
	 * @param  companyId the primary key of the company
	 * @return the matching participant
	 * @throws PortalException if a matching participant could not be found
	 * @throws SystemException if a system exception occurred
	 */
	@Override
	public Participant getParticipantByUuidAndCompanyId(String uuid,
		long companyId) throws PortalException, SystemException {
		return participantPersistence.findByUuid_C_First(uuid, companyId, null);
	}

	/**
	 * Returns the participant matching the UUID and group.
	 *
	 * @param uuid the participant's UUID
	 * @param groupId the primary key of the group
	 * @return the matching participant
	 * @throws PortalException if a matching participant could not be found
	 * @throws SystemException if a system exception occurred
	 */
	@Override
	public Participant getParticipantByUuidAndGroupId(String uuid, long groupId)
		throws PortalException, SystemException {
		return participantPersistence.findByUUID_G(uuid, groupId);
	}

	/**
	 * Returns a range of all the participants.
	 *
	 * <p>
	 * Useful when paginating results. Returns a maximum of <code>end - start</code> instances. <code>start</code> and <code>end</code> are not primary keys, they are indexes in the result set. Thus, <code>0</code> refers to the first result in the set. Setting both <code>start</code> and <code>end</code> to {@link com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS} will return the full result set. If <code>orderByComparator</code> is specified, then the query will include the given ORDER BY logic. If <code>orderByComparator</code> is absent and pagination is required (<code>start</code> and <code>end</code> are not {@link com.liferay.portal.kernel.dao.orm.QueryUtil#ALL_POS}), then the query will include the default ORDER BY logic from {@link com.rivetlogic.event.model.impl.ParticipantModelImpl}. If both <code>orderByComparator</code> and pagination are absent, for performance reasons, the query will not have an ORDER BY clause and the returned result set will be sorted on by the primary key in an ascending order.
	 * </p>
	 *
	 * @param start the lower bound of the range of participants
	 * @param end the upper bound of the range of participants (not inclusive)
	 * @return the range of participants
	 * @throws SystemException if a system exception occurred
	 */
	@Override
	public List<Participant> getParticipants(int start, int end)
		throws SystemException {
		return participantPersistence.findAll(start, end);
	}

	/**
	 * Returns the number of participants.
	 *
	 * @return the number of participants
	 * @throws SystemException if a system exception occurred
	 */
	@Override
	public int getParticipantsCount() throws SystemException {
		return participantPersistence.countAll();
	}

	/**
	 * Updates the participant in the database or adds it if it does not yet exist. Also notifies the appropriate model listeners.
	 *
	 * @param participant the participant
	 * @return the participant that was updated
	 * @throws SystemException if a system exception occurred
	 */
	@Indexable(type = IndexableType.REINDEX)
	@Override
	public Participant updateParticipant(Participant participant)
		throws SystemException {
		return participantPersistence.update(participant);
	}

	/**
	 * Returns the event local service.
	 *
	 * @return the event local service
	 */
	public com.rivetlogic.event.service.EventLocalService getEventLocalService() {
		return eventLocalService;
	}

	/**
	 * Sets the event local service.
	 *
	 * @param eventLocalService the event local service
	 */
	public void setEventLocalService(
		com.rivetlogic.event.service.EventLocalService eventLocalService) {
		this.eventLocalService = eventLocalService;
	}

	/**
	 * Returns the event persistence.
	 *
	 * @return the event persistence
	 */
	public EventPersistence getEventPersistence() {
		return eventPersistence;
	}

	/**
	 * Sets the event persistence.
	 *
	 * @param eventPersistence the event persistence
	 */
	public void setEventPersistence(EventPersistence eventPersistence) {
		this.eventPersistence = eventPersistence;
	}

	/**
	 * Returns the participant local service.
	 *
	 * @return the participant local service
	 */
	public com.rivetlogic.event.service.ParticipantLocalService getParticipantLocalService() {
		return participantLocalService;
	}

	/**
	 * Sets the participant local service.
	 *
	 * @param participantLocalService the participant local service
	 */
	public void setParticipantLocalService(
		com.rivetlogic.event.service.ParticipantLocalService participantLocalService) {
		this.participantLocalService = participantLocalService;
	}

	/**
	 * Returns the participant persistence.
	 *
	 * @return the participant persistence
	 */
	public ParticipantPersistence getParticipantPersistence() {
		return participantPersistence;
	}

	/**
	 * Sets the participant persistence.
	 *
	 * @param participantPersistence the participant persistence
	 */
	public void setParticipantPersistence(
		ParticipantPersistence participantPersistence) {
		this.participantPersistence = participantPersistence;
	}

	/**
	 * Returns the token local service.
	 *
	 * @return the token local service
	 */
	public com.rivetlogic.event.service.TokenLocalService getTokenLocalService() {
		return tokenLocalService;
	}

	/**
	 * Sets the token local service.
	 *
	 * @param tokenLocalService the token local service
	 */
	public void setTokenLocalService(
		com.rivetlogic.event.service.TokenLocalService tokenLocalService) {
		this.tokenLocalService = tokenLocalService;
	}

	/**
	 * Returns the token persistence.
	 *
	 * @return the token persistence
	 */
	public TokenPersistence getTokenPersistence() {
		return tokenPersistence;
	}

	/**
	 * Sets the token persistence.
	 *
	 * @param tokenPersistence the token persistence
	 */
	public void setTokenPersistence(TokenPersistence tokenPersistence) {
		this.tokenPersistence = tokenPersistence;
	}

	/**
	 * Returns the counter local service.
	 *
	 * @return the counter local service
	 */
	public com.liferay.counter.service.CounterLocalService getCounterLocalService() {
		return counterLocalService;
	}

	/**
	 * Sets the counter local service.
	 *
	 * @param counterLocalService the counter local service
	 */
	public void setCounterLocalService(
		com.liferay.counter.service.CounterLocalService counterLocalService) {
		this.counterLocalService = counterLocalService;
	}

	/**
	 * Returns the resource local service.
	 *
	 * @return the resource local service
	 */
	public com.liferay.portal.service.ResourceLocalService getResourceLocalService() {
		return resourceLocalService;
	}

	/**
	 * Sets the resource local service.
	 *
	 * @param resourceLocalService the resource local service
	 */
	public void setResourceLocalService(
		com.liferay.portal.service.ResourceLocalService resourceLocalService) {
		this.resourceLocalService = resourceLocalService;
	}

	/**
	 * Returns the user local service.
	 *
	 * @return the user local service
	 */
	public com.liferay.portal.service.UserLocalService getUserLocalService() {
		return userLocalService;
	}

	/**
	 * Sets the user local service.
	 *
	 * @param userLocalService the user local service
	 */
	public void setUserLocalService(
		com.liferay.portal.service.UserLocalService userLocalService) {
		this.userLocalService = userLocalService;
	}

	/**
	 * Returns the user remote service.
	 *
	 * @return the user remote service
	 */
	public com.liferay.portal.service.UserService getUserService() {
		return userService;
	}

	/**
	 * Sets the user remote service.
	 *
	 * @param userService the user remote service
	 */
	public void setUserService(
		com.liferay.portal.service.UserService userService) {
		this.userService = userService;
	}

	/**
	 * Returns the user persistence.
	 *
	 * @return the user persistence
	 */
	public UserPersistence getUserPersistence() {
		return userPersistence;
	}

	/**
	 * Sets the user persistence.
	 *
	 * @param userPersistence the user persistence
	 */
	public void setUserPersistence(UserPersistence userPersistence) {
		this.userPersistence = userPersistence;
	}

	public void afterPropertiesSet() {
		Class<?> clazz = getClass();

		_classLoader = clazz.getClassLoader();

		PersistedModelLocalServiceRegistryUtil.register("com.rivetlogic.event.model.Participant",
			participantLocalService);
	}

	public void destroy() {
		PersistedModelLocalServiceRegistryUtil.unregister(
			"com.rivetlogic.event.model.Participant");
	}

	/**
	 * Returns the Spring bean ID for this bean.
	 *
	 * @return the Spring bean ID for this bean
	 */
	@Override
	public String getBeanIdentifier() {
		return _beanIdentifier;
	}

	/**
	 * Sets the Spring bean ID for this bean.
	 *
	 * @param beanIdentifier the Spring bean ID for this bean
	 */
	@Override
	public void setBeanIdentifier(String beanIdentifier) {
		_beanIdentifier = beanIdentifier;
	}

	@Override
	public Object invokeMethod(String name, String[] parameterTypes,
		Object[] arguments) throws Throwable {
		Thread currentThread = Thread.currentThread();

		ClassLoader contextClassLoader = currentThread.getContextClassLoader();

		if (contextClassLoader != _classLoader) {
			currentThread.setContextClassLoader(_classLoader);
		}

		try {
			return _clpInvoker.invokeMethod(name, parameterTypes, arguments);
		}
		finally {
			if (contextClassLoader != _classLoader) {
				currentThread.setContextClassLoader(contextClassLoader);
			}
		}
	}

	protected Class<?> getModelClass() {
		return Participant.class;
	}

	protected String getModelClassName() {
		return Participant.class.getName();
	}

	/**
	 * Performs an SQL query.
	 *
	 * @param sql the sql query
	 */
	protected void runSQL(String sql) throws SystemException {
		try {
			DataSource dataSource = participantPersistence.getDataSource();

			SqlUpdate sqlUpdate = SqlUpdateFactoryUtil.getSqlUpdate(dataSource,
					sql, new int[0]);

			sqlUpdate.update();
		}
		catch (Exception e) {
			throw new SystemException(e);
		}
	}

	@BeanReference(type = com.rivetlogic.event.service.EventLocalService.class)
	protected com.rivetlogic.event.service.EventLocalService eventLocalService;
	@BeanReference(type = EventPersistence.class)
	protected EventPersistence eventPersistence;
	@BeanReference(type = com.rivetlogic.event.service.ParticipantLocalService.class)
	protected com.rivetlogic.event.service.ParticipantLocalService participantLocalService;
	@BeanReference(type = ParticipantPersistence.class)
	protected ParticipantPersistence participantPersistence;
	@BeanReference(type = com.rivetlogic.event.service.TokenLocalService.class)
	protected com.rivetlogic.event.service.TokenLocalService tokenLocalService;
	@BeanReference(type = TokenPersistence.class)
	protected TokenPersistence tokenPersistence;
	@BeanReference(type = com.liferay.counter.service.CounterLocalService.class)
	protected com.liferay.counter.service.CounterLocalService counterLocalService;
	@BeanReference(type = com.liferay.portal.service.ResourceLocalService.class)
	protected com.liferay.portal.service.ResourceLocalService resourceLocalService;
	@BeanReference(type = com.liferay.portal.service.UserLocalService.class)
	protected com.liferay.portal.service.UserLocalService userLocalService;
	@BeanReference(type = com.liferay.portal.service.UserService.class)
	protected com.liferay.portal.service.UserService userService;
	@BeanReference(type = UserPersistence.class)
	protected UserPersistence userPersistence;
	private String _beanIdentifier;
	private ClassLoader _classLoader;
	private ParticipantLocalServiceClpInvoker _clpInvoker = new ParticipantLocalServiceClpInvoker();
}