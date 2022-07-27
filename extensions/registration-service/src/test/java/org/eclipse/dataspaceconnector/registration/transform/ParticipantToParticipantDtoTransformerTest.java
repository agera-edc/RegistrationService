package org.eclipse.dataspaceconnector.registration.transform;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.eclipse.dataspaceconnector.registration.authority.dto.ParticipantDto;
import org.eclipse.dataspaceconnector.registration.authority.dto.ParticipantStatusDto;
import org.eclipse.dataspaceconnector.registration.authority.model.Participant;
import org.eclipse.dataspaceconnector.registration.authority.model.ParticipantStatus;
import org.eclipse.dataspaceconnector.spi.transformer.TransformerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.dataspaceconnector.registration.TestUtils.createParticipant;
import static org.mockito.Mockito.mock;

public class ParticipantToParticipantDtoTransformerTest {

    private final ParticipantToParticipantDtoTransformer transformer = new ParticipantToParticipantDtoTransformer();

    @Test
    void inputOutputType() {
        assertThat(transformer.getInputType()).isEqualTo(Participant.class);
        assertThat(transformer.getOutputType()).isEqualTo(ParticipantDto.class);
    }

    @ParameterizedTest
    @MethodSource("participantStatus")
    void transform(ParticipantStatus status) {
        var context = mock(TransformerContext.class);

        var participant = createParticipant().status(status).build();
        var participantDto = transformer.transform(participant, context);

        // ignoring status filed as it mapped to specific statuses in DTO.
        assertThat(participantDto)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoredFields("status")
                        .build())
                .isEqualTo(participant);

        assertThat(participantDto.getStatus())
                .matches(statusDto -> (
                                Arrays.stream(ParticipantStatusDto.values())
                                        .filter(val -> val.equals(statusDto)).count() == 1
                        )
                );
    }

    private static Stream<Arguments> participantStatus() {
        return Arrays.stream(ParticipantStatus.values())
                .map(Arguments::of);
    }
}
