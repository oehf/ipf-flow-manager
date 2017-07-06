# IPF Flow Manager
==================

[![Build Status](https://travis-ci.org/oehf/ipf-flow-manager.svg?branch=master)](https://travis-ci.org/oehf/ipf-flow-manager)

## Concept

The flow manager is a service that monitors and controls application-specific message flows. 
It stores each incoming message as a flow object in a database and updates the flow object as the message travels through the integration application.
If the message was processed and has been successfully delivered to a destination system, an acknowledgement (ACK) is stored with that particular message flow.
If the processing of a message has failed, a negative acknowledgement (NAK) is stored with that message flow.

<img src="https://github.com/oehf/ipf-flow-manager/blob/master/documentation/images/flowmgr.JPG">

With the flow manager you can also replay messages. A replay re-submits the initially stored message.
In order to avoid duplicate delivery of messages to destination systems you can install duplicate filters.
These recognize replayed messages that have already been delivered to an external destination and filter them out.
Duplicate filtering can be turned on and off at runtime via the JMX interface.
Message replays can be useful if a destination system is unavailable for a longer time than covered by a redelivery policy of internal message buffers
In this case the buffer gives up redelivery and leaves the administrator with the duty to run a manual redelivery (replay) once the destination system is available again.
Note that a message that has already been acknowledged will by default not be re-delivered to the destination system.
This allows for non-idempotent message receivers.
Only NAKed messages or unacknowledged message flows can be sent to a destination system during a replay, otherwise they are filtered out.
However, the duplicate filtering functionality can be turned on and off for each application individually.
The flow manager can also store string representations of messages that enter and leave an integration solution.
These string representations are rendered at the flow interceptors.
Application developers can provide custom renderers if needed, i.e. message rendering can be customized when a message flow is initialized, acknowledged or negatively acknowledged.

<img src="https://github.com/oehf/ipf-flow-manager/blob/master/documentation/images/flowmanager_rendering.jpg">

Stored string representations of inbound- and outbound messages (also called incoming- and outgoing messages) allow administrators and auditors to keep track which messages entered and left the system.
These string representations can be visualized via a generic JMX client.
Also, you can perform fulltext searches based on the string representations of messages.Messages can also be encrypted in the database and still be searched via fulltext message searches.
