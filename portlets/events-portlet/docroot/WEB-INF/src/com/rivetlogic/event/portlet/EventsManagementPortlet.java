/** 
 * Copyright (C) 2005-2014 Rivet Logic Corporation. 
 * 
 * This program is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License 
 * as published by the Free Software Foundation; version 3 
 * of the License. 
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 * GNU General Public License for more details. 
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the Free Software 
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, 
 * Boston, MA 02110-1301, USA. 
 */

package com.rivetlogic.event.portlet;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageBusUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.upload.UploadPortletRequest;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.MimeTypesUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.util.bridges.mvc.MVCPortlet;
import com.rivetlogic.event.beans.ManagementPrefsBean;
import com.rivetlogic.event.model.Event;
import com.rivetlogic.event.model.Participant;
import com.rivetlogic.event.model.impl.EventImpl;
import com.rivetlogic.event.model.impl.ParticipantImpl;
import com.rivetlogic.event.notification.constant.EventPortletConstants;
import com.rivetlogic.event.notification.constant.NotificationConstants;
import com.rivetlogic.event.notification.constant.PreferencesConstants;
import com.rivetlogic.event.service.EventLocalServiceUtil;
import com.rivetlogic.event.service.ParticipantLocalServiceUtil;
import com.rivetlogic.event.util.EventActionUtil;
import com.rivetlogic.event.util.EventConstant;
import com.rivetlogic.event.util.EventValidator;
import com.rivetlogic.event.util.WebKeys;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletRequest;
import javax.portlet.ReadOnlyException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ValidatorException;

/**
 * @author charlesrodriguez
 * @author christopherjimenez
 * @author juancarrillo
 */
public class EventsManagementPortlet extends MVCPortlet {
    
    public void addEditEvent(ActionRequest request, ActionResponse response) throws PortalException, SystemException,
        PortletException, IOException {
        
        Event event;
        
        if (Validator.isNull(ParamUtil.getLong(request, EventPortletConstants.PARAMETER_RESOURCE_PRIMARY_KEY))) {
            event = createEvent(request, response);
        } else {
            event = editEvent(request, response);
        }
        
        if (!SessionErrors.isEmpty(request)) {
            request.setAttribute(WebKeys.EVENT_ENTRY, event);
            response.setRenderParameter(WebKeys.REDIRECT, PortalUtil.getCurrentURL(request));
            response.setRenderParameter(WebKeys.MVC_PATH, WebKeys.EDIT_EVENT_PAGE);
        }
    }
    
    public void changePreferences(ActionRequest request, ActionResponse response) throws IOException {
        
        try {
            ManagementPrefsBean prefBean = new ManagementPrefsBean(request);
            prefBean.save(request);
            request.setAttribute(WebKeys.PREF_BEAN, prefBean);
            
        } catch (ReadOnlyException roe) {
            _log.error(PreferencesConstants.ERROR_LOAD_PREFERENCES, roe);
            SessionErrors.add(request, PreferencesConstants.ERROR_LOAD_PREFERENCES);
        } catch (ValidatorException ve) {
            _log.error(PreferencesConstants.ERROR_WRITE_PERFERENCES, ve);
            SessionErrors.add(request, PreferencesConstants.ERROR_WRITE_PERFERENCES);
        } catch (IOException ioe) {
            _log.error(PreferencesConstants.ERROR_IO_PREFERENCES, ioe);
            SessionErrors.add(request, PreferencesConstants.ERROR_IO_PREFERENCES);
        }
    }
    
    public Event createEvent(ActionRequest request, ActionResponse response) throws PortletException, IOException,
        PortalException, SystemException {
        
        UploadPortletRequest upreq = PortalUtil.getUploadPortletRequest(request);
        
        ThemeDisplay themeDisplay = (ThemeDisplay) upreq.getAttribute(WebKeys.THEME_DISPLAY);
        
        Calendar newEventDate = getDateFromRequest(request, upreq, themeDisplay);
        
        EventImpl event = new EventImpl();
        event.setCompanyId(themeDisplay.getCompanyId());
        event.setGroupId(themeDisplay.getCompanyGroupId());
        event.setUserId(themeDisplay.getUserId());
        
        event.setName(ParamUtil.getString(upreq, EventPortletConstants.PARAMETER_NAME));
        event.setLocation(ParamUtil.getString(upreq, EventPortletConstants.PARAMETER_LOCATION));
        event.setDescription(ParamUtil.getString(upreq, EventPortletConstants.PARAMETER_DESCRIPTION));
        event.setEventDate(newEventDate.getTime());
        event.setPrivateEvent(ParamUtil.getBoolean(upreq, EventPortletConstants.PARAMETER_EVENT));
        
        List<String> errors = new ArrayList<String>();
        EventValidator.validateEvent(event, errors);
        EventActionUtil.setErrors(errors, request);
        
        List<Participant> participants = createParticipants(request, upreq, themeDisplay, event);
        
        if (SessionErrors.isEmpty(request)) {
            saveNewEvent(request, themeDisplay, event, participants);
        }
        
        return event;
    }
    
    public List<Participant> createParticipants(ActionRequest request, UploadPortletRequest upreq,
            ThemeDisplay themeDisplay, Event event) throws PortalException, SystemException, PortletException,
        IOException {
        
        List<Participant> participants = new ArrayList<Participant>();
        
        List<String> repeatedEmails = new ArrayList<String>();
        List<String> invalidEmails = new ArrayList<String>();
        
        getParticipantsByCSV(request, upreq, participants, repeatedEmails, invalidEmails, event.getEventId());
        
        getParticipansByForm(upreq, request, participants, repeatedEmails, invalidEmails, event.getEventId());
        
        if (!SessionErrors.isEmpty(request)) {
            request.setAttribute(WebKeys.REPEATED_EMAILS, repeatedEmails);
            request.setAttribute(WebKeys.INVALID_EMAILS, invalidEmails);
        }
        
        return participants;
    }
    
    public void deleteEvent(ActionRequest request, ActionResponse response) throws PortletException, IOException,
        PortalException, SystemException {
        
        ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
        Long eventId = ParamUtil.getLong(request, NotificationConstants.EVENT_ID);
        Event event = EventLocalServiceUtil.deleteEvent(eventId);
        sendCancelNotification(request, event, themeDisplay, null);
        response.setRenderParameter(WebKeys.MVC_PATH, WebKeys.EVENT_MANAGEMENT_VIEW_PAGE);
    }
    
    public void deleteParticipant(ActionRequest request, ActionResponse response) throws PortletException, IOException,
        PortalException, SystemException {
        ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
        Participant p = ParticipantLocalServiceUtil.deleteParticipant(ParamUtil.getLong(request,
                NotificationConstants.PARTICIPANT_ID));
        
        sendSingleCancelledEventNotification(request, p, themeDisplay);
        
        response.setRenderParameter(EventPortletConstants.PARAMETER_RESOURCE_PRIMARY_KEY,
                ParamUtil.getString(request, EventPortletConstants.PARAMETER_RESOURCE_PRIMARY_KEY));
        response.setRenderParameter(WebKeys.MVC_PATH, WebKeys.EVENT_MANAGEMENT_EDIT_PAGE);
    }
    
    @Override
    public void doEdit(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        
        String selectedTab = ParamUtil.getString(request, WebKeys.SELECTED_TAB, PreferencesConstants.EMAIL_FROM);
        
        request.setAttribute(WebKeys.SELECTED_TAB, selectedTab);
        request.setAttribute(WebKeys.PREF_BEAN, new ManagementPrefsBean(request));
        
        super.doEdit(request, response);
    }
    
    @Override
    public void render(RenderRequest request, RenderResponse response) throws PortletException, IOException {
        
        String mvcPath = ParamUtil.getString(request, WebKeys.MVC_PATH);
        
        if (mvcPath.equals(WebKeys.EDIT_EVENT_PAGE)) {
            EventActionUtil.loadEvent(request);
        }
        ManagementPrefsBean prefBean = new ManagementPrefsBean(request);
        request.setAttribute(WebKeys.PREF_BEAN, prefBean);
        super.render(request, response);
    }
    
    public void sendReminder(ActionRequest request, ActionResponse response) throws PortalException, SystemException {
        
        Long eventId = ParamUtil.getLong(request, NotificationConstants.EVENT_ID, EventPortletConstants.INVALID_ID);
        if (eventId != -1) {
            Event event = EventLocalServiceUtil.getEvent(eventId);
            ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
            sendReminder(request, event, themeDisplay);
        } else {
            _log.error(ERROR_REQUEST_MISSING_EVENTID);
        }
    }
    
    private void calculateInvitations(PortletRequest request, Event event, List<Participant> participants,
            ThemeDisplay themeDisplay, boolean eventChanged) throws SystemException, PortalException {
        
        Map<String, Participant> removedParticipants = new HashMap<String, Participant>();
        
        // All current participants will be removed
        List<Participant> currentParticipants = event.getParticipants();
        for (Participant cp : currentParticipants) {
            ParticipantLocalServiceUtil.deleteParticipant(cp.getParticipantId());
            removedParticipants.put(cp.getEmail(), cp);
        }
        
        // All new participants must be added
        for (Participant p : participants) {
            
            // Checking if it was participant before
            Participant cp = removedParticipants.get(p.getEmail());
            
            // If the added participant was removed before and there are not
            // changes in the event, we are goig to keep their previous status
            if (cp != null && !eventChanged) {
                p.setStatus(cp.getStatus());
            }
            
            // Add participant
            ParticipantLocalServiceUtil.addParticipant(p);
            
            // If the added participant was removed before 
            if (cp != null) {
                
                //and the event changed, we send an update notification
                if (eventChanged){
                    sendSingleUpdateInvitation(request, p, themeDisplay);
                }
                
                // remove participant to avoid send cancelation notification
                removedParticipants.remove(p.getEmail());
            }
            // If is a new participant we send an new event notification
            else {
                sendRegularInvitation(request, p, themeDisplay);
            }
        }
        
        // To all definitively removed participants, we send a cancelation
        // notification
        sendCancelNotification(request, event, themeDisplay, new ArrayList<Participant>(removedParticipants.values()));
    }
    
    private boolean checkIsCSV(ActionRequest request, InputStream inputStream, String fileName) {
        boolean isCSV = false;
        
        if (Validator.isNotNull(inputStream) && Validator.isNotNull(fileName)) {
            String mimeType = MimeTypesUtil.getContentType(inputStream, fileName);
            
            if (_log.isDebugEnabled())
                _log.debug(DEBUG_MESSAGE_MIME_TYPE + mimeType);
            
            if (mimeType.equals(ContentTypes.TEXT_CSV) || mimeType.equals(ContentTypes.TEXT_CSV_UTF8)) {
                
                isCSV = true;
                
            } else {
                SessionErrors.add(request, ERROR_INVALID_CSV);
            }
        }
        
        return isCSV;
    }
    
    private Event editEvent(ActionRequest request, ActionResponse response) throws PortletException, IOException,
        PortalException, SystemException {
        
        UploadPortletRequest upreq = PortalUtil.getUploadPortletRequest(request);
        
        ThemeDisplay themeDisplay = (ThemeDisplay) upreq.getAttribute(WebKeys.THEME_DISPLAY);
        
        Calendar newEventDate = getDateFromRequest(request, upreq, themeDisplay);
        
        Event dbEvent = EventLocalServiceUtil.getEvent(ParamUtil.getLong(upreq,
                EventPortletConstants.PARAMETER_RESOURCE_PRIMARY_KEY));
        
        Event event = (Event) dbEvent.clone();
        
        event.setName(ParamUtil.getString(upreq, EventPortletConstants.PARAMETER_NAME));
        event.setLocation(ParamUtil.getString(upreq, EventPortletConstants.PARAMETER_LOCATION));
        event.setDescription(ParamUtil.getString(upreq, EventPortletConstants.PARAMETER_DESCRIPTION));
        event.setEventDate(newEventDate.getTime());
        event.setPrivateEvent(ParamUtil.getBoolean(upreq, EventPortletConstants.PARAMETER_EVENT));
        
        List<String> errors = new ArrayList<String>();
        EventValidator.validateEvent(event, errors);
        EventActionUtil.setErrors(errors, request);
        
        List<Participant> participants = new ArrayList<Participant>();
        
        boolean eventChanged = !dbEvent.equals(event);
        
        boolean useCSV = updateParticipants(request, upreq, event, participants);
        
        if (SessionErrors.isEmpty(request)) {
            saveEditEventChanges(event, participants, useCSV, eventChanged, themeDisplay, request);
        }
        
        return event;
    }
    
    private Calendar getDateFromRequest(ActionRequest request, UploadPortletRequest upreq, ThemeDisplay themeDisplay) {
        
        Calendar calendar = new GregorianCalendar();
        
        boolean dateIsValid = false;
        
        String eventDateString = ParamUtil.getString(upreq, EventPortletConstants.PARAMETER_EVENT_DATE);
        
        if (Validator.isNotNull(eventDateString)) {
            DateFormat dateFormat = new SimpleDateFormat(SIMPLE_DATE_FORMAT);
            try {
                Date date = dateFormat.parse(eventDateString);
                calendar.setTime(date);
                dateIsValid = true;
            } catch (ParseException e) {
                SessionErrors.add(request, ERROR_INVALID_DATE);
            }
            
        } else {
            SessionErrors.add(request, ERROR_DATE_REQUIRED);
        }
        
        int ampm = ParamUtil.getInteger(upreq, EventPortletConstants.PARAMETER_AMPM, -1);
        int hour = ParamUtil.getInteger(upreq, EventPortletConstants.PARAMETER_HOUR, -1);
        int min = ParamUtil.getInteger(upreq, EventPortletConstants.PARAMETER_MIN, -1);
        
        if (ampm != -1 && hour != -1 && min != -1) {
            if (ampm == 1) {
                // hour += 12;
            }
            calendar.set(Calendar.AM_PM, ampm);
            calendar.set(Calendar.HOUR, hour);
            calendar.set(Calendar.MINUTE, min);
        } else {
            dateIsValid = false;
            SessionErrors.add(request, ERROR_HOUR_REQUIRED);
        }
        
        if (dateIsValid) {
            Calendar today = new GregorianCalendar(themeDisplay.getTimeZone(), themeDisplay.getLocale());
            
            if (today.after(calendar)) {
                SessionErrors.add(request, ERROR_DATE_FUTURE);
            }
        }
        
        return calendar;
    }
    
    private void getFileParticipants(ActionRequest request, InputStream inputStream, UploadPortletRequest upreq,
            List<Participant> participants, List<String> repeatedEmails, List<String> invalidEmails, long eventId)
        throws IOException {
        
        ThemeDisplay themeDisplay = (ThemeDisplay) upreq.getAttribute(WebKeys.THEME_DISPLAY);
        
        String strLine = null;
        DataInputStream in = new DataInputStream(inputStream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        
        while ((strLine = br.readLine()) != null) {
            List<String> errors = new ArrayList<String>();
            Participant participant = new ParticipantImpl();
            String[] values = strLine.split(SEPARATOR);
            
            participant.setEventId(eventId);
            participant.setFullName(values[0].trim());
            participant.setEmail(values[1].trim());
            
            participant.setCompanyId(themeDisplay.getCompanyId());
            participant.setGroupId(themeDisplay.getScopeGroupId());
            participant.setCompanyName(EventConstant.DEFAULT_COMPANY);
            participant.setPhoneNumber(EventConstant.DEFAULT_PHONE_NUMBER);
            
            if (EventValidator
                    .validateParticipantInfo(participant, participants, errors, repeatedEmails, invalidEmails)) {
                
                participants.add(participant);
            }
            
            EventActionUtil.setErrors(errors, request);
        }
    }
    
    private void getParticipansByForm(UploadPortletRequest upreq, ActionRequest request,
            List<Participant> participants, List<String> repeatedEmails, List<String> invalidEmails, long eventId)
        throws PortletException, IOException, PortalException, SystemException {
        
        ThemeDisplay themeDisplay = (ThemeDisplay) upreq.getAttribute(WebKeys.THEME_DISPLAY);
        
        String newParticipants = ParamUtil.getString(upreq, WebKeys.PARTICIPANT_INDEXES);
        
        request.removeAttribute(WebKeys.PARTICIPANT_INDEXES);
        
        request.setAttribute(WebKeys.PARTICIPANT_INDEXES, newParticipants);
        
        List<Participant> formParticipants = new ArrayList<Participant>();
        
        int[] quantity = StringUtil.split(newParticipants, 0);
        
        if (_log.isDebugEnabled())
            _log.debug(DEBUG_NEW_PARTICIPANTS + quantity.length);
        
        for (int i : quantity) {
            String name, email = null;
            
            name = ParamUtil.getString(upreq, (EventPortletConstants.PARAMETER_PARTICIPANT_FULL_NAME + i));
            email = ParamUtil.getString(upreq, (EventPortletConstants.PARAMETER_PARTICIPANT_EMAIL + i));
            
            if (Validator.isNotNull(name) && Validator.isNotNull(email)) {
                
                List<String> errors = new ArrayList<String>();
                Participant participant = new ParticipantImpl();
                
                participant.setEventId(eventId);
                participant.setCompanyId(themeDisplay.getCompanyId());
                participant.setGroupId(themeDisplay.getScopeGroupId());
                participant.setFullName(name);
                participant.setEmail(email);
                participant.setCompanyName(EventConstant.DEFAULT_COMPANY);
                participant.setPhoneNumber(EventConstant.DEFAULT_PHONE_NUMBER);
                
                EventValidator.validateRegisteredParticipant(participant, participants, errors, repeatedEmails,
                        invalidEmails);
                
                participants.add(participant);
                formParticipants.add(participant);
                
                EventActionUtil.setErrors(errors, request);
            }
        }
        
        request.setAttribute(WebKeys.PARTICIPANTS, formParticipants);
    }
    
    private void getParticipantsByCSV(ActionRequest request, UploadPortletRequest upreq,
            List<Participant> participants, List<String> repeatedEmails, List<String> invalidEmails, long eventId) {
        
        try {
            String fileName = upreq.getFileName(EventPortletConstants.PARAMETER_FILE);
            
            if (checkIsCSV(request, upreq.getFileAsStream(EventPortletConstants.PARAMETER_FILE), fileName)) {
                getFileParticipants(request, upreq.getFileAsStream(EventPortletConstants.PARAMETER_FILE), upreq,
                        participants, repeatedEmails, invalidEmails, eventId);
            }
            
        } catch (IOException e) {
            _log.error(e);
            SessionErrors.add(request, ERROR_PROCESING_CSV);
        }
    }
    
    private void saveEditEventChanges(Event event, List<Participant> participants, boolean useCSV,
            boolean eventChanged, ThemeDisplay themeDisplay, ActionRequest request) {
        
        try {
            EventLocalServiceUtil.updateEvent(event);
            
            if (useCSV) {
                calculateInvitations(request, event, participants, themeDisplay, eventChanged);
                
            } else {
                
                for (Participant p : participants) {
                    ParticipantLocalServiceUtil.addParticipant(p);
                    sendRegularInvitation(request, p, themeDisplay);
                }
                
                if (eventChanged) {
                    sendUpdateNotification(request, event, themeDisplay);
                }
            }
            
        } catch (Exception e) {
            _log.error(e);
            SessionErrors.add(request, ERROR_SAVE_EVENT);
        }
    }
    
    private void saveNewEvent(ActionRequest request, ThemeDisplay themeDisplay, Event event,
            List<Participant> participants) {
        try {
            Event eventAdded = EventLocalServiceUtil.addEvent(event);
            for (Participant participant : participants) {
                participant.setEventId(eventAdded.getEventId());
                ParticipantLocalServiceUtil.addParticipant(participant);
                sendRegularInvitation(request, participant, themeDisplay);
            }
            
        } catch (Exception e) {
            SessionErrors.add(request, ERROR_SAVE_EVENT);
            _log.error(e);
        }
    }
    
    private void sendCancelNotification(PortletRequest request, Event event, ThemeDisplay themeDisplay,
            List<Participant> participants) {
        
        ManagementPrefsBean prefBean = new ManagementPrefsBean(request);
        Message message = new Message();
        message.put(NotificationConstants.CMD, NotificationConstants.EVENT_CANCELLED);
        message.put(NotificationConstants.SENDER_NAME, prefBean.getNameFrom());
        message.put(NotificationConstants.SENDER, prefBean.getEmailFrom());
        message.put(NotificationConstants.BODY_TEMPLATE, prefBean.getCancelledEventNotificationBody());
        message.put(NotificationConstants.SUBJECT_TEMPLATE, prefBean.getCancelledEventNotificationSubject());
        message.put(NotificationConstants.PUBLIC_URL, prefBean.getPublicEventsURL());
        message.put(NotificationConstants.PORTAL_URL, PortalUtil.getPortalURL(request));
        message.put(NotificationConstants.EVENT, event);
        message.put(NotificationConstants.PARTICIPANTS, participants);
        
        MessageBusUtil.sendMessage(NotificationConstants.SEND_NOTIFICATION_DESTINATION, message);
    }
    
    private void sendRegularInvitation(PortletRequest request, Participant participant, ThemeDisplay themeDisplay) {
        
        ManagementPrefsBean prefBean = new ManagementPrefsBean(request);
        Message message = new Message();
        message.put(NotificationConstants.CMD, NotificationConstants.REGULAR_INVITATION);
        message.put(NotificationConstants.SENDER, prefBean.getEmailFrom());
        message.put(NotificationConstants.SENDER_NAME, prefBean.getNameFrom());
        message.put(NotificationConstants.RECIPIENTS, participant.getEmail());
        message.put(NotificationConstants.BODY_TEMPLATE, prefBean.getRegularInvitationBody());
        message.put(NotificationConstants.SUBJECT_TEMPLATE, prefBean.getRegularInvitationSubject());
        message.put(NotificationConstants.PUBLIC_URL, prefBean.getPublicEventsURL());
        message.put(NotificationConstants.PORTAL_URL, PortalUtil.getPortalURL(request));
        message.put(NotificationConstants.PARTICIPANT_ID, participant.getParticipantId());
        message.put(NotificationConstants.EVENT_ID, participant.getEventId());
        
        MessageBusUtil.sendMessage(NotificationConstants.SEND_NOTIFICATION_DESTINATION, message);
    }
    
    private void sendReminder(PortletRequest request, Event event, ThemeDisplay themeDisplay) {
        
        ManagementPrefsBean prefBean = new ManagementPrefsBean(request);
        Message message = new Message();
        message.put(NotificationConstants.CMD, NotificationConstants.REMINDER_MESSAGE);
        message.put(NotificationConstants.SENDER_NAME, prefBean.getNameFrom());
        message.put(NotificationConstants.SENDER, prefBean.getEmailFrom());
        message.put(NotificationConstants.BODY_TEMPLATE, prefBean.getReminderEventBody());
        message.put(NotificationConstants.SUBJECT_TEMPLATE, prefBean.getReminderEventSubject());
        message.put(NotificationConstants.PUBLIC_URL, prefBean.getPublicEventsURL());
        message.put(NotificationConstants.PORTAL_URL, PortalUtil.getPortalURL(request));
        message.put(NotificationConstants.EVENT_ID, event.getEventId());
        
        MessageBusUtil.sendMessage(NotificationConstants.SEND_NOTIFICATION_DESTINATION, message);
    }
    
    private void sendSingleCancelledEventNotification(PortletRequest request, Participant participant,
            ThemeDisplay themeDisplay) {
        
        ManagementPrefsBean prefBean = new ManagementPrefsBean(request);
        Message message = new Message();
        message.put(NotificationConstants.CMD, NotificationConstants.SINGLE_CANCELLED_EVENT);
        message.put(NotificationConstants.SENDER_NAME, prefBean.getNameFrom());
        message.put(NotificationConstants.SENDER, prefBean.getEmailFrom());
        message.put(NotificationConstants.RECIPIENTS, participant.getEmail());
        message.put(NotificationConstants.BODY_TEMPLATE, prefBean.getCancelledEventNotificationBody());
        message.put(NotificationConstants.SUBJECT_TEMPLATE, prefBean.getCancelledEventNotificationSubject());
        message.put(NotificationConstants.PUBLIC_URL, prefBean.getPublicEventsURL());
        message.put(NotificationConstants.PORTAL_URL, PortalUtil.getPortalURL(request));
        message.put(NotificationConstants.PARTICIPANT, participant);
        message.put(NotificationConstants.EVENT_ID, participant.getEventId());
        
        MessageBusUtil.sendMessage(NotificationConstants.SEND_NOTIFICATION_DESTINATION, message);
    }
    
    private void sendSingleUpdateInvitation(PortletRequest request, Participant participant, ThemeDisplay themeDisplay) {
        
        ManagementPrefsBean prefBean = new ManagementPrefsBean(request);
        Message message = new Message();
        message.put(NotificationConstants.CMD, NotificationConstants.SINGLE_EVENT_UPDATE);
        message.put(NotificationConstants.SENDER_NAME, prefBean.getNameFrom());
        message.put(NotificationConstants.SENDER, prefBean.getEmailFrom());
        message.put(NotificationConstants.RECIPIENTS, participant.getEmail());
        message.put(NotificationConstants.BODY_TEMPLATE, prefBean.getUpdatedEventInvitationBody());
        message.put(NotificationConstants.SUBJECT_TEMPLATE, prefBean.getUpdatedEventInvitationSubject());
        message.put(NotificationConstants.PUBLIC_URL, prefBean.getPublicEventsURL());
        message.put(NotificationConstants.PORTAL_URL, PortalUtil.getPortalURL(request));
        message.put(NotificationConstants.PARTICIPANT, participant);
        message.put(NotificationConstants.EVENT_ID, participant.getEventId());
        
        MessageBusUtil.sendMessage(NotificationConstants.SEND_NOTIFICATION_DESTINATION, message);
    }
    
    private void sendUpdateNotification(PortletRequest request, Event event, ThemeDisplay themeDisplay) {
        
        ManagementPrefsBean prefBean = new ManagementPrefsBean(request);
        Message message = new Message();
        message.put(NotificationConstants.CMD, NotificationConstants.EVENT_UPDATED);
        message.put(NotificationConstants.SENDER_NAME, prefBean.getNameFrom());
        message.put(NotificationConstants.SENDER, prefBean.getEmailFrom());
        message.put(NotificationConstants.BODY_TEMPLATE, prefBean.getUpdatedEventInvitationBody());
        message.put(NotificationConstants.SUBJECT_TEMPLATE, prefBean.getUpdatedEventInvitationSubject());
        message.put(NotificationConstants.PUBLIC_URL, prefBean.getPublicEventsURL());
        message.put(NotificationConstants.PORTAL_URL, PortalUtil.getPortalURL(request));
        message.put(NotificationConstants.EVENT_ID, event.getEventId());
        
        MessageBusUtil.sendMessage(NotificationConstants.SEND_NOTIFICATION_DESTINATION, message);
    }
    
    private boolean updateParticipants(ActionRequest request, UploadPortletRequest upreq, Event event,
            List<Participant> participants) throws SystemException, IOException, PortalException, PortletException {
        
        boolean useCSV = false;
        
        List<String> repeatedEmails = new ArrayList<String>();
        List<String> invalidEmails = new ArrayList<String>();
        
        getParticipantsByCSV(request, upreq, participants, repeatedEmails, invalidEmails, event.getEventId());
        
        if (!participants.isEmpty()) {
            useCSV = true;
        }
        
        getParticipansByForm(upreq, request, participants, repeatedEmails, invalidEmails, event.getEventId());
        
        if (!SessionErrors.isEmpty(request)) {
            request.setAttribute(WebKeys.REPEATED_EMAILS, repeatedEmails);
            request.setAttribute(WebKeys.INVALID_EMAILS, invalidEmails);
        }
        
        return useCSV;
    }
    
    private static final String ERROR_SAVE_EVENT = "event-save-error";
    private static final String ERROR_INVALID_CSV = "invalid-csv-file";
    private static final String ERROR_PROCESING_CSV = "error-processing-csv";
    private static final String ERROR_HOUR_REQUIRED = "event-hour-required";
    private static final String ERROR_DATE_FUTURE = "event-date-in-future";
    private static final String ERROR_INVALID_DATE = "event-invalid-date";
    private static final String ERROR_DATE_REQUIRED = "event-date-required";
    private static final String ERROR_REQUEST_MISSING_EVENTID = "Error on request eventId is not present";
    private static final String DEBUG_MESSAGE_MIME_TYPE = "File MIME type: ";
    private static final String DEBUG_NEW_PARTICIPANTS = "newParticipants: ";
    private static final String SEPARATOR = ",";
    private static final String SIMPLE_DATE_FORMAT = "MM/dd/yyyy";
    private static final Log _log = LogFactoryUtil.getLog(EventsManagementPortlet.class);
}
