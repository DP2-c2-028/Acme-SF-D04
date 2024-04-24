
package acme.features.auditor.codeAudit;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import acme.client.data.models.Dataset;
import acme.client.services.AbstractService;
import acme.client.views.SelectChoices;
import acme.entities.auditRecords.AuditRecord;
import acme.entities.auditRecords.Mark;
import acme.entities.codeAudits.CodeAudit;
import acme.entities.codeAudits.CodeAuditType;
import acme.entities.projects.Project;
import acme.roles.Auditor;

@Service
public class AuditorCodeAuditPublishService extends AbstractService<Auditor, CodeAudit> {

	// Internal state ---------------------------------------------------------

	@Autowired
	private AuditorCodeAuditRepository repository;

	// AbstractService interface ----------------------------------------------


	@Override
	public void authorise() {
		boolean status;
		int id;
		int auditorId;
		CodeAudit codeAudit;

		id = super.getRequest().getData("id", int.class);
		codeAudit = this.repository.findOneCodeAuditById(id);

		auditorId = super.getRequest().getPrincipal().getActiveRoleId();

		status = auditorId == codeAudit.getAuditor().getId() && codeAudit.isDraftMode();

		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		CodeAudit object;
		int id;

		id = super.getRequest().getData("id", int.class);
		object = this.repository.findOneCodeAuditById(id);

		super.getBuffer().addData(object);
	}

	@Override
	public void bind(final CodeAudit object) {
		assert object != null;

		super.bind(object, "code", "execution", "type", "proposedCorrectiveActions", "link", "project");
	}

	@Override
	public void validate(final CodeAudit object) {
		assert object != null;
		if (!super.getBuffer().getErrors().hasErrors("mark")) {
			List<Mark> marks;

			marks = this.repository.findMarksByCodeAuditId(object.getId()).stream().toList();
			Mark nota = this.repository.averageMark(marks);

			super.state(nota == Mark.C || nota == Mark.B || nota == Mark.A || nota == Mark.AA, "mark", "auditor.code-audit.form.error.mark");
		}

		if (!super.getBuffer().getErrors().hasErrors("unpublishedAuditRecords")) {

			Collection<AuditRecord> unpublishedAuditRecords;

			unpublishedAuditRecords = this.repository.findUnpublishedAuditRecordsByCodeAuditId(object.getId());

			super.state(unpublishedAuditRecords.isEmpty(), "*", "auditor.code-audit.form.error.unpublished-audit-records");
		}
	}

	@Override
	public void perform(final CodeAudit object) {
		assert object != null;

		object.setDraftMode(false);
		this.repository.save(object);
	}

	@Override
	public void unbind(final CodeAudit object) {
		assert object != null;

		Dataset dataset;
		SelectChoices choices;
		Collection<Project> projects = this.repository.findProjects();
		SelectChoices choices2;
		List<Mark> marks;

		marks = this.repository.findMarksByCodeAuditId(object.getId()).stream().toList();

		choices = SelectChoices.from(CodeAuditType.class, object.getType());
		choices2 = SelectChoices.from(projects, "code", (Project) projects.toArray()[0]);

		dataset = super.unbind(object, "code", "execution", "type", "proposedCorrectiveActions", "link", "project", "draftMode");
		dataset.put("mark", this.repository.averageMark(marks));
		dataset.put("types", choices);
		dataset.put("projects", choices2);

		super.getResponse().addData(dataset);
	}
}
