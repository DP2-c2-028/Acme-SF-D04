
package acme.features.auditor.codeAudit;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import acme.client.repositories.AbstractRepository;
import acme.entities.auditRecords.AuditRecord;
import acme.entities.auditRecords.Mark;
import acme.entities.codeAudits.CodeAudit;
import acme.entities.projects.Project;
import acme.roles.Auditor;

@Repository
public interface AuditorCodeAuditRepository extends AbstractRepository {

	@Query("select ca from CodeAudit ca where ca.auditor.id = :id")
	Collection<CodeAudit> findCodeAuditsByAuditorId(int id);

	@Query("select ca from CodeAudit ca where ca.id = :id")
	CodeAudit findOneCodeAuditById(int id);

	@Query("select a from Auditor a where a.id = :id")
	Auditor findOneAuditorById(int id);

	@Query("select ca from CodeAudit ca where ca.code=:code")
	CodeAudit findOneCodeAuditByCode(String code);

	@Query("select ad from AuditRecord ad where ad.codeAudit.id=:codeAuditId")
	Collection<AuditRecord> findRelationsByCodeAuditId(int codeAuditId);

	@Query("select ad.mark from AuditRecord ad where ad.codeAudit.id=:codeAuditId")
	Collection<Mark> findMarksByCodeAuditId(int codeAuditId);

	@Query("select p from Project p")
	Collection<Project> findProjects();

	default int traducirANumero(final Mark mark) {
		int res = 0;
		if (mark == Mark.FF)
			res = 0;
		else if (mark == Mark.F)
			res = 1;
		else if (mark == Mark.C)
			res = 2;
		else if (mark == Mark.B)
			res = 3;
		else if (mark == Mark.A)
			res = 4;
		else
			res = 5;

		return res;
	}

	default Mark traducirANota(final double media) {
		Mark res = Mark.C;
		double average = Math.round(media);
		if (average < 1)
			res = Mark.FF;
		else if (average < 2 && average >= 1)
			res = Mark.F;
		else if (average < 3 && average >= 2)
			res = Mark.C;
		else if (average < 4 && average >= 3)
			res = Mark.B;
		else if (average < 5 && average >= 4)
			res = Mark.A;
		else
			res = Mark.AA;
		return res;
	}

	default Mark averageMark(final List<Mark> ls) {
		double average = ls.stream().mapToDouble(m -> this.traducirANumero(m)).average().orElse(0);
		Mark res = this.traducirANota(average);
		return res;
	}
}
