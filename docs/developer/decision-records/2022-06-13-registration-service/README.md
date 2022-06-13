# Registration Service

Registration Service is a component of the dataspace providing Dataspace Authority. In current version the Dataspace Authority is centralised. In future 
versions, the Authority could also be partly or fully decentralized.

Responsibilities of the Dataspace Authority:
- Establishes a DID defining the Dataspace.
- Manages policies defining who is allowed to participate in the dataspace.
- Publishes a self-description document.
- Manages enrollment process of the Dataspace participants.
- Lists the available Dataspace participants.

## Architecture 

The MVD's Registration Service will be written in Java and re-use the runtime framework and modules from EDC. This enables us to leverage the same functionality such as policy validation, re-use domain objects and architectural principles.

Potentially long-running operations such as onboarding and offboarding must be asynchronous and are handled using the state machine concept. The state 
machine is operated in such a way that domain objects are loaded from storage, processed and then put back into storage to make the registration service runtime stateless.

In the MVD version the Registration Service will use in-memory storage.

## Deployment

The Registration Service will be deployed as a separate component in the Dataspace environment.

## Identity

the Registration Service will have its own `did:web` deployed that points to the Self-description document endpoint. From the Self-description document 
potential Dataspace participants can retrieve the enrollment API endpoint.

## Operations

This document specifies only operations supported by the Registration Service in MVD version:
- Dataspace participant onboarding
- Listing all Dataspace participants

Other operations that will be supported in future versions of the Registration Service:
- Dataspace participant offboarding
- Blacklisting

### 1. Onboarding

#### Participants

1. Company1, an entity which intends to become a Dataspace participant
2. The Dataspace Authority, which manages the enrollment process

#### Overview

A Client for Company1 initiates the enrollment process by resolving and contacting the enrollment API endpoint for the Dataspace Authority. The client could be e.g. a CLI utility.

The Dataspace Authority enrollment service obtains Verifiable Credentials from Company1 to determine whether it meets enrollment policies. The enrollment service then issues a Verifiable Credential that establishes membership and pushes it to Company 1's Identity Hub, and stores membership and certificate information.

In simple scenarios, enrollement could be fast and fully automated. However, in advanced scenarios, enrollment policies could require interactions with external systems, and even manual processes. Therefore, it is implemented asynchronously.

There could be different "types" of onboarding, e.g. onboarding a participant or a credential issuer, so the architecture has to support that.

#### Pre-conditions

1. A root CA is established and trusted by all participants. (Intermediate CAs are out of scope in this simplified discussion)
2. Company1 knows the DID URL of the Dataspace it intends to join.
3. The Dataspace Authority Identity Hub contains a VC signed by the root CA, establishing the Dataspace Authority DID as the effective Dataspace Authority for Dataspace D.
4. The Company1 Identity Hub contains VCs that satisfy the Dataspace Authority enrollment policy. For example, it could be a credential signed by the German Government that establishes Company1 to be based in Germany, and a credential signed by Auditor1 that establishes Company1 to be ISO27001 certified.

#### Post-conditions

1. The Company1 Identity Hub contains a VC (X.509 certificate) signed by the Dataspace Authority, that establishes membership in Dataspace D. This is used by other participants to authorize requests from Company1.
2. The X.509 certificate is stored in the Dataspace Authority Certificate Log. This is used for auditing and revocation.
3. The Company1 DID URL is stored in the Dataspace Authority Participant Registry. This is used to serve participant requests.

#### Flow sequence

![dataspace-enrollment](dataspace-enrollment.png)

1. The Client for Company1 initiates the enrollment process based on the Dataspace DID URL. It retrieves the DID Document, and parses it to determine the Self-description document endpoint.
2. The Client for Company1 retrieves the Self-description document, and parses it to retrieve the Dataspace enrollment HTTP endpoint.
3. The client needs access to the Company1 Private Key to sign a JWS. The client sends an HTTP request to the Dataspace Authority enrollement endpoint. The request is accepted for asynchronous processing.
4. The DA uses the Distributed authorization sub-flow (see above) to authenticate the request...
5. ... and retrieve credentials from Company1's Identity Hub.
6. The DA authorizes the request by applying the Dataspace enrollment policy on the obtained Verifiable Credentials.
7. The DA stores membership information in its registry. At the very least, this includes Company 1's DID URL.
8. The DA issues and signs a membership Verifiable Credential as an X.509 Certificate.
9. The DA stores the Certificate in its log, for audit and revocation.
10. The DA sends the Verifiable Credential to Company1's Identity Hub for storage. It uses the Identity Hub bearer token (from the Distributed authorization sub-flow) to authenticate the request.
11. Company1's Identity Hub validates the bearer token and stores the membership Verifiable Credential.

### List participants flow

#### Participants

1. Company1, a Dataspace Participant with an EDC application that wants to discover IDS endpoints (for instance, in order to list contract offers)
2. The Dataspace Authority, which manages the participant registry
3. Company2, Company3, etc., Dataspace Participants

#### Overview

A typical EDC deployment caches contract offers from other participants in a federated catalog, so that users can quickly browse and negotiate contracts. To regularly retrieve offers, it regularly contacts the Dataspace Registry to refresh its list of Dataspace Participants, then obtains contract offers from each participants to refresh its cache.

In this flow, the EDC for Company1 obtains a list of Dataspace Participants and resolves their IDS endpoints. Using these IDS endpoints (e.g. for listing contract offers) is outside the scope of this flow (see *IDS Flows* below).

#### Pre-conditions

1. Participants are registered as (currently valid) Dataspace Participants

#### Post-conditions

None

#### Flow sequence

![list-participants](list-participants.png)

1. The EDC for Company1 determines the Self-description document endpoint from the Dataspace DID Document.
2. The EDC for Company1 determines the Dataspace Registry endpoint from the Self-description document.
3. The EDC for Company1 issues a request to the Dataspace Registry, to list participants.
4. The Registry uses the Distributed authorization sub-flow (see above) to authenticate the request...
5. ... and retrieve credentials from Company1's Identity Hub.
6. The Registry authorizes the request by applying the Registry access policy on the obtained Verifiable Credentials. For example, the caller must be a valid Dataspace Participant.
7. The Registry obtains the list of Dataspace Participant DID URIs from its storage...
8. ... and returns it synchronously to the caller (Company1 EDC).
9. The EDC for Company1 iterates through the Participants' DID URIs, and retrieves the collection of their IDS endpoints from their DID Documents.