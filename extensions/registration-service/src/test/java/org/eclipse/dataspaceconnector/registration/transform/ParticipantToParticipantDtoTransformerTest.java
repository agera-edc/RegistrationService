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
    void transform(ParticipantStatus status, ParticipantStatusDto expectedDtoStatus) {
        var context = mock(TransformerContext.class);

        var participant = createParticipant().status(status).build();
        var participantDto = transformer.transform(participant, context);

        assertThat(participantDto).isNotNull();
        // ignoring status field as it is mapped to specific status in DTO.
        assertThat(participantDto)
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoredFields("status")
                        .build())
                .isEqualTo(participant);
        // comparing dto status
        assertThat(participantDto.getStatus()).isEqualTo(expectedDtoStatus);
    }
    
    private static Stream<Arguments> participantStatus() {
        return Stream.of(
                Arguments.of(ParticipantStatus.ONBOARDING_INITIATED, ParticipantStatusDto.AUTHORIZING),
                Arguments.of(ParticipantStatus.AUTHORIZING, ParticipantStatusDto.AUTHORIZING),
                Arguments.of(ParticipantStatus.AUTHORIZED, ParticipantStatusDto.AUTHORIZED),
                Arguments.of(ParticipantStatus.DENIED, ParticipantStatusDto.DENIED)
        );
    }
}
