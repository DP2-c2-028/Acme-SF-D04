
package acme.features.client.dashboard;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import acme.client.data.datatypes.Money;
import acme.client.data.models.Dataset;
import acme.client.services.AbstractService;
import acme.forms.ClientDashboard;
import acme.roles.Client;

@Service
public class ClientDashboardShowService extends AbstractService<Client, ClientDashboard> {

	// Internal state ---------------------------------------------------------

	@Autowired
	private ClientDashboardRepository repository;

	// AbstractService interface ----------------------------------------------


	@Override
	public void authorise() {
		super.getResponse().setAuthorised(true);
	}

	@Override
	public void load() {
		ClientDashboard dashboard;
		int totalLogsWithCompletenessBelow25;
		int totalLogsWithCompletenessBetween25And50;
		int totalLogsWithCompletenessBetween50And75;
		int totalLogsWithCompletenessAbove75;
		int clientId;

		Double AverageBudgetOfContracts;
		Double MinimunBudgetOfContracts;
		Double MaximunBudgetOfContracts;

		clientId = super.getRequest().getPrincipal().getActiveRoleId();
		double percentaje25 = 25.0;
		double percentaje50 = 50.0;
		double percentaje75 = 75.0;

		totalLogsWithCompletenessBelow25 = this.repository.logsBelowCompletenessValue(clientId, percentaje25);
		totalLogsWithCompletenessBetween25And50 = this.repository.logsBetweenCompletenessValuesForClient(clientId, percentaje25, percentaje50);
		totalLogsWithCompletenessBetween50And75 = this.repository.logsBetweenCompletenessValuesForClient(clientId, percentaje50, percentaje75);
		totalLogsWithCompletenessAbove75 = this.repository.logsAboveCompletenessValue(clientId, percentaje75);

		//Contracts
		Collection<Money> contractBudgets = this.repository.findAllBudgetsFromClient(clientId);

		if (!contractBudgets.isEmpty()) {
			AverageBudgetOfContracts = contractBudgets.stream().mapToDouble(u -> this.repository.currencyTransformerUsd(u)).average().orElse(0.0);
			MinimunBudgetOfContracts = contractBudgets.stream().mapToDouble(u -> this.repository.currencyTransformerUsd(u)).min().orElse(0.0);
			MaximunBudgetOfContracts = contractBudgets.stream().mapToDouble(u -> this.repository.currencyTransformerUsd(u)).max().orElse(0.0);
		} else {

			AverageBudgetOfContracts = null;
			MinimunBudgetOfContracts = null;
			MaximunBudgetOfContracts = null;
		}
		dashboard = new ClientDashboard();

		// Progress Logs
		dashboard.setTotalLogsWithCompletenessBelow25(totalLogsWithCompletenessBelow25);
		dashboard.setTotalLogsWithCompletenessBetween25And50(totalLogsWithCompletenessBetween25And50);
		dashboard.setTotalLogsWithCompletenessBetween50And75(totalLogsWithCompletenessBetween50And75);
		dashboard.setTotalLogsWithCompletenessAbove75(totalLogsWithCompletenessAbove75);

		dashboard.setAverageBudgetOfContracts(AverageBudgetOfContracts);
		dashboard.setMinimunBudgetOfContracts(MinimunBudgetOfContracts);
		dashboard.setMaximunBudgetOfContracts(MaximunBudgetOfContracts);
		dashboard.setDeviationOfContractBudgets(this.invoicesDeviationQuantity(contractBudgets));

		super.getBuffer().addData(dashboard);
	}

	private double invoicesDeviationQuantity(final Collection<Money> quantites) {
		double deviation;

		double average = quantites.stream().mapToDouble(u -> this.repository.currencyTransformerUsd(u)).average().orElse(0.0);

		double sumOfSquares = quantites.stream().mapToDouble(x -> Math.pow(x.getAmount() - average, 2)).sum();

		double vari = sumOfSquares / quantites.size();

		double dev = Math.sqrt(vari);

		deviation = dev;

		return deviation;
	}

	@Override
	public void unbind(final ClientDashboard clientDashboard) {
		Dataset dataset;

		dataset = super.unbind(clientDashboard, //
			"totalLogsWithCompletenessBelow25", "totalLogsWithCompletenessBetween25And50", // 
			"totalLogsWithCompletenessBetween50And75", "totalLogsWithCompletenessAbove75", //
			"averageBudgetOfContracts", "deviationOfContractBudgets",//
			"minimunBudgetOfContracts", "maximunBudgetOfContracts");

		super.getResponse().addData(dataset);
	}
}