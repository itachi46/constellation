/*
 * Copyright 2010-2019 Australian Signals Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package au.gov.asd.tac.constellation.algorithms.sna.metrics;

import au.gov.asd.tac.constellation.graph.GraphWriteMethods;
import au.gov.asd.tac.constellation.graph.schema.SchemaAttribute;
import au.gov.asd.tac.constellation.pluginframework.Plugin;
import au.gov.asd.tac.constellation.pluginframework.PluginInfo;
import au.gov.asd.tac.constellation.pluginframework.PluginInteraction;
import au.gov.asd.tac.constellation.pluginframework.parameters.PluginParameter;
import au.gov.asd.tac.constellation.pluginframework.parameters.PluginParameters;
import au.gov.asd.tac.constellation.pluginframework.parameters.types.BooleanParameterType;
import au.gov.asd.tac.constellation.pluginframework.parameters.types.BooleanParameterType.BooleanParameterValue;
import au.gov.asd.tac.constellation.pluginframework.templates.SimpleEditPlugin;
import au.gov.asd.tac.constellation.schema.analyticschema.concept.AnalyticConcept;
import java.util.HashMap;
import java.util.Map;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * Calculates weight for each pair of vertices. This importance measure does not
 * include loops.
 *
 * @author canis_majoris
 */
@ServiceProvider(service = Plugin.class)
@NbBundle.Messages("WeightPlugin=Weight")
@PluginInfo(tags = {"ANALYTIC"})
public class WeightPlugin extends SimpleEditPlugin {

    private static final SchemaAttribute WEIGHT_ATTRIBUTE = AnalyticConcept.TransactionAttribute.WEIGHT;

    public static final String NORMALISE_AVAILABLE_PARAMETER_ID = PluginParameter.buildId(WeightPlugin.class, "normalise_available");

    @Override
    public PluginParameters createParameters() {
        final PluginParameters parameters = new PluginParameters();

        final PluginParameter<BooleanParameterValue> normaliseToScoresParameter = BooleanParameterType.build(NORMALISE_AVAILABLE_PARAMETER_ID);
        normaliseToScoresParameter.setName("Normalise By Max Available Score");
        normaliseToScoresParameter.setDescription("Normalise calculated scores by the maximum calculated score");
        normaliseToScoresParameter.setBooleanValue(false);
        parameters.addParameter(normaliseToScoresParameter);

        return parameters;
    }

    @Override
    public void edit(final GraphWriteMethods graph, final PluginInteraction interaction, final PluginParameters parameters) throws InterruptedException {
        final boolean normaliseByAvailable = parameters.getBooleanValue(NORMALISE_AVAILABLE_PARAMETER_ID);

        // calculate weight for every pair of vertices on the graph
        float maxWeight = 0;
        final Map<Integer, Float> weights = new HashMap<>();
        final int linkCount = graph.getLinkCount();
        for (int linkPosition = 0; linkPosition < linkCount; linkPosition++) {
            final int linkId = graph.getLink(linkPosition);
            final float weight = (float) graph.getLinkTransactionCount(linkId);

            weights.put(linkId, weight);
            maxWeight = Math.max(weight, maxWeight);

        }

        // update the graph with weight values
        final int weightAttribute = WEIGHT_ATTRIBUTE.ensure(graph);
        for (int linkId : weights.keySet()) {
            final int transactionCount = graph.getLinkTransactionCount(linkId);
            for (int transactionPosition = 0; transactionPosition < transactionCount; transactionPosition++) {
                final int transactionId = graph.getLinkTransaction(linkId, transactionPosition);
                if (normaliseByAvailable && maxWeight > 0) {
                    graph.setFloatValue(weightAttribute, transactionId, weights.get(linkId) / maxWeight);
                } else {
                    graph.setFloatValue(weightAttribute, transactionId, weights.get(linkId));
                }
            }
        }
    }
}
