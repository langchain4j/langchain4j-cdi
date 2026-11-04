/** */
package dev.langchain4j.cdi.core;

import dev.langchain4j.model.decision.DecisionModel;
import dev.langchain4j.model.decision.request.DecisionRequest;
import dev.langchain4j.model.decision.response.DecisionResponse;

/** */
public class DummyDecisionModel implements DecisionModel {

    @Override
    public DecisionResponse doDecide(DecisionRequest request) {
        // TODO Auto-generated method stub
        throw new RuntimeException("Not implemented");
    }
}
