package org.example.schemaflow.shared.domain.entities;

public enum CollaborationMessageType {
    // Gestión de usuarios
    USER_JOINED,
    USER_LEFT,
    CONNECTED_USERS,

    // Operaciones con nodos
    NODE_CREATED,
    NODE_UPDATED,
    NODE_DELETED,
    NODE_LOCKED,
    NODE_UNLOCKED,

    // Operaciones con edges
    EDGE_CREATED,
    EDGE_UPDATED,
    EDGE_DELETED,

    // Interacción en tiempo real
    USER_CURSOR,
    SELECTION_CHANGED,

    // Sistema
    ERROR,
    PING,
    LOCKED_NODES, PONG
}
