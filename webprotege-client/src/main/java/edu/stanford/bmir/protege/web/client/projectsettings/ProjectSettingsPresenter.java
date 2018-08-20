package edu.stanford.bmir.protege.web.client.projectsettings;

import com.google.common.collect.ImmutableList;
import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.event.shared.EventBus;
import edu.stanford.bmir.protege.web.client.Messages;
import edu.stanford.bmir.protege.web.client.app.PermissionScreener;
import edu.stanford.bmir.protege.web.client.crud.EntityCrudKitSettingsEditor;
import edu.stanford.bmir.protege.web.client.dispatch.DispatchServiceCallback;
import edu.stanford.bmir.protege.web.client.dispatch.DispatchServiceManager;
import edu.stanford.bmir.protege.web.client.lang.DefaultDictionaryLanguageView;
import edu.stanford.bmir.protege.web.client.lang.DefaultDisplayNameSettingsView;
import edu.stanford.bmir.protege.web.client.renderer.AnnotationPropertyIriRenderer;
import edu.stanford.bmir.protege.web.client.settings.SettingsPresenter;
import edu.stanford.bmir.protege.web.shared.DataFactory;
import edu.stanford.bmir.protege.web.shared.crud.EntityCrudKitSettings;
import edu.stanford.bmir.protege.web.shared.crud.GetEntityCrudKitSettingsAction;
import edu.stanford.bmir.protege.web.shared.crud.IRIPrefixUpdateStrategy;
import edu.stanford.bmir.protege.web.shared.crud.SetEntityCrudKitSettingsAction;
import edu.stanford.bmir.protege.web.shared.entity.OWLAnnotationPropertyData;
import edu.stanford.bmir.protege.web.shared.lang.DisplayNameSettings;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.shared.projectsettings.*;
import edu.stanford.bmir.protege.web.shared.shortform.DictionaryLanguage;
import org.semanticweb.owlapi.model.IRI;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static edu.stanford.bmir.protege.web.shared.access.BuiltInAction.EDIT_PROJECT_SETTINGS;

/**
 * Matthew Horridge
 * Stanford Center for Biomedical Informatics Research
 * 7 Jun 2017
 */
public class ProjectSettingsPresenter {

    private final ProjectId projectId;

    private final PermissionScreener permissionScreener;

    private final DispatchServiceManager dispatchServiceManager;

    private final EventBus eventBus;

    @Nonnull
    private final SettingsPresenter settingsPresenter;

    @Nonnull
    private final GeneralSettingsView generalSettingsView;

    @Nonnull
    private final DefaultDictionaryLanguageView defaultDictionaryLanguageView;

    @Nonnull
    private final DefaultDisplayNameSettingsView defaultDisplayNameSettingsView;

    @Nonnull
    private final EntityCrudKitSettingsEditor entityCrudKitSettingsEditor;

    @Nonnull
    private final SlackWebhookSettingsView slackWebhookSettingsView;

    @Nonnull
    private final WebhookSettingsView webhookSettingsView;

    @Nonnull
    private final Messages messages;

    @Nonnull
    private final AnnotationPropertyIriRenderer annotationPropertyIriRenderer;

    @Inject
    public ProjectSettingsPresenter(@Nonnull ProjectId projectId,
                                    @Nonnull PermissionScreener permissionScreener,
                                    @Nonnull DispatchServiceManager dispatchServiceManager,
                                    @Nonnull EventBus eventBus,
                                    @Nonnull SettingsPresenter settingsPresenter,
                                    @Nonnull GeneralSettingsView generalSettingsView,
                                    @Nonnull DefaultDictionaryLanguageView defaultDictionaryLanguageView,
                                    @Nonnull DefaultDisplayNameSettingsView defaultDisplayNameSettingsView,
                                    @Nonnull EntityCrudKitSettingsEditor entityCrudKitSettingsEditor, @Nonnull SlackWebhookSettingsView slackWebhookSettingsView,
                                    @Nonnull WebhookSettingsView webhookSettingsView,
                                    @Nonnull Messages messages, @Nonnull AnnotationPropertyIriRenderer annotationPropertyIriRenderer) {
        this.projectId = checkNotNull(projectId);
        this.permissionScreener = checkNotNull(permissionScreener);
        this.dispatchServiceManager = checkNotNull(dispatchServiceManager);
        this.eventBus = checkNotNull(eventBus);
        this.settingsPresenter = checkNotNull(settingsPresenter);
        this.generalSettingsView = checkNotNull(generalSettingsView);
        this.defaultDictionaryLanguageView = checkNotNull(defaultDictionaryLanguageView);
        this.defaultDisplayNameSettingsView = checkNotNull(defaultDisplayNameSettingsView);
        this.entityCrudKitSettingsEditor = checkNotNull(entityCrudKitSettingsEditor);
        this.slackWebhookSettingsView = checkNotNull(slackWebhookSettingsView);
        this.webhookSettingsView = checkNotNull(webhookSettingsView);
        this.messages = checkNotNull(messages);
        this.annotationPropertyIriRenderer = checkNotNull(annotationPropertyIriRenderer);
    }

    public ProjectId getProjectId() {
        return projectId;
    }


    public void start(AcceptsOneWidget container) {
        permissionScreener.checkPermission(EDIT_PROJECT_SETTINGS.getActionId(),
                                           container,
                                           () -> displaySettings(container));

    }

    public void setNextPlace(@Nonnull Optional<Place> nextPlace) {
        settingsPresenter.setNextPlace(checkNotNull(nextPlace));
    }

    private void displaySettings(AcceptsOneWidget container) {
        settingsPresenter.setSettingsTitle(messages.projectSettings_title());
        settingsPresenter.start(container);
        settingsPresenter.setApplySettingsHandler(this::applySettings);
        settingsPresenter.setCancelSettingsHandler(this::handleCancel);
        settingsPresenter.addSection(messages.projectSettings_mainSettings()).setWidget(generalSettingsView);
        // TODO: Check that the user can do this
        settingsPresenter.addSection(messages.newEntitySettings()).setWidget(entityCrudKitSettingsEditor);
        settingsPresenter.addSection(messages.language_defaultSettings_title()).setWidget(defaultDictionaryLanguageView);
        settingsPresenter.addSection("Default Display Name Settings").setWidget(defaultDisplayNameSettingsView);
        settingsPresenter.addSection(messages.projectSettings_slackWebHookUrl()).setWidget(slackWebhookSettingsView);
        settingsPresenter.addSection(messages.projectSettings_payloadUrls()).setWidget(webhookSettingsView);
        settingsPresenter.setBusy(container, true);
        dispatchServiceManager.execute(new GetProjectSettingsAction(projectId),
                                       result -> {
                                           ProjectSettings projectSettings = result.getProjectSettings();
                                           displayProjectSettings(container, projectSettings);
                                       });
        dispatchServiceManager.execute(new GetEntityCrudKitSettingsAction(projectId),
                                       result -> {
                                           EntityCrudKitSettings<?> settings = result.getSettings();
                                           entityCrudKitSettingsEditor.setValue(settings);
                                       });
    }

    private void displayProjectSettings(@Nonnull AcceptsOneWidget container,
                                        @Nonnull ProjectSettings projectSettings) {
        generalSettingsView.setDisplayName(projectSettings.getProjectDisplayName());
        generalSettingsView.setDescription(projectSettings.getProjectDescription());

        displayDefaultDictionaryLanguage(projectSettings.getDefaultLanguage());
        displayDefaultDisplayNameLanguages(projectSettings.getDefaultDisplayNameSettings());
        SlackIntegrationSettings slackIntegrationSettings = projectSettings.getSlackIntegrationSettings();
        slackWebhookSettingsView.setWebhookUrl(slackIntegrationSettings.getPayloadUrl());
        WebhookSettings webhookSettings = projectSettings.getWebhookSettings();
        webhookSettingsView.setWebhookUrls(webhookSettings.getWebhookSettings());
        settingsPresenter.setBusy(container, false);
    }

    private void displayDefaultDictionaryLanguage(@Nonnull DictionaryLanguage defaultLanguage) {
        IRI annotationPropertyIri = defaultLanguage.getAnnotationPropertyIri();
        if (annotationPropertyIri != null) {
            annotationPropertyIriRenderer.renderAnnotationPropertyIri(annotationPropertyIri, defaultDictionaryLanguageView::setAnnotationProperty);
        }
        else {
            defaultDictionaryLanguageView.clearAnnotationProperty();
        }
        defaultDictionaryLanguageView.setLanguageTag(defaultLanguage.getLang());
    }

    private void displayDefaultDisplayNameLanguages(@Nonnull DisplayNameSettings displayNameSettings) {
        defaultDisplayNameSettingsView.setPrimaryLanguages(displayNameSettings.getPrimaryDisplayNameLanguages());
    }


    private void applySettings() {
        SlackIntegrationSettings slackIntegrationSettings = SlackIntegrationSettings.get(slackWebhookSettingsView.getWebhookrUrl());
        WebhookSettings webhookSettings = WebhookSettings.get(webhookSettingsView.getWebhookUrls());
        ProjectSettings projectSettings = ProjectSettings.get(
                projectId,
                generalSettingsView.getDisplayName(),
                generalSettingsView.getDescription(),
                getDefaultLanguage(),
                getDefaultDisplayNameSettings(),
                slackIntegrationSettings,
                webhookSettings
        );
        dispatchServiceManager.execute(new SetProjectSettingsAction(projectSettings), new DispatchServiceCallback<SetProjectSettingsResult>() {
            @Override
            public void handleSuccess(SetProjectSettingsResult setProjectSettingsResult) {
                eventBus.fireEvent(new ProjectSettingsChangedEvent(projectSettings).asGWTEvent());
                settingsPresenter.goToNextPlace();
            }
        });
        entityCrudKitSettingsEditor.getValue().ifPresent(settings -> {
            dispatchServiceManager.execute(new SetEntityCrudKitSettingsAction(projectId,
                                                                              settings, settings,
                                                                              IRIPrefixUpdateStrategy.LEAVE_INTACT),
                                           result -> {});
        });

    }

    private DictionaryLanguage getDefaultLanguage() {
        OWLAnnotationPropertyData property = defaultDictionaryLanguageView.getAnnotationProperty()
                                                                          .orElse(DataFactory.getRdfsLabelData());
        String langTag = defaultDictionaryLanguageView.getLanguageTag();
        return DictionaryLanguage.create(property.getEntity().getIRI(), langTag);
    }

    private DisplayNameSettings getDefaultDisplayNameSettings() {
        return DisplayNameSettings.get(
                defaultDisplayNameSettingsView.getPrimaryLanguages(),
                ImmutableList.of()
        );
    }

    private void handleCancel() {

    }
}
