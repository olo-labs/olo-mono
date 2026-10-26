/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import type { CSSProperties } from 'react'
import { Handle, Position } from '@xyflow/react'
import {
  portDisplayColor,
  portDisplayLabel,
  portTooltipLines,
} from '../../lib/portConnection'
import { PLANNER_ROUTED_MESSAGE_PORT_COLOR, isMessagePortId, type PortSide } from '../../lib/workflowNodePorts'
import type { WorkflowPort } from '../../types/workflow'

export function handlePosition(side: PortSide): Position {
  switch (side) {
    case 'TOP':
      return Position.Top
    case 'BOTTOM':
      return Position.Bottom
    case 'RIGHT':
      return Position.Right
    default:
      return Position.Left
  }
}

function portSlotPercent(index: number, count: number): string {
  return `${((index + 1) / (count + 1)) * 100}%`
}

export function PortRow({
  port,
  side,
  index,
  count,
  portColors,
  inline = false,
  dimmed = false,
}: {
  port: WorkflowPort
  side: PortSide
  index: number
  count: number
  portColors: Record<string, string>
  inline?: boolean
  dimmed?: boolean
}) {
  const color = dimmed
    ? PLANNER_ROUTED_MESSAGE_PORT_COLOR
    : portDisplayColor(port, portColors)
  const isInput = port.direction === 'INPUT'
  const position = handlePosition(side)
  const tooltipLines = [
    ...portTooltipLines(port),
    ...(dimmed ? ['Connected dynamically via planner/agent at runtime'] : []),
  ]
  const slot = portSlotPercent(index, count)
  const handleStyle: CSSProperties = inline
    ? { top: '50%', ...(color ? { background: color, borderColor: color } : {}) }
    : side === 'LEFT' || side === 'RIGHT'
      ? { top: slot, ...(color ? { background: color, borderColor: color } : {}) }
      : { left: slot, ...(color ? { background: color, borderColor: color } : {}) }

  const rowClass = [
    inline
      ? `catalog-flow-port-row ${isInput ? 'catalog-flow-port-row-input' : 'catalog-flow-port-row-output'} catalog-flow-port-row-inline-${side.toLowerCase()}`
      : `catalog-flow-port-row catalog-flow-port-row-side-${side.toLowerCase()}`,
    dimmed ? 'catalog-flow-port-row-dimmed' : '',
  ].filter(Boolean).join(' ')

  return (
    <div
      className={rowClass}
      style={inline ? undefined : ({ '--port-slot': slot } as CSSProperties)}
    >
      <Handle
        type={isInput ? 'target' : 'source'}
        position={position}
        id={port.id}
        isConnectable={!dimmed}
        className={`catalog-flow-handle catalog-flow-handle-${isInput ? 'input' : 'output'}${dimmed ? ' catalog-flow-handle-dimmed' : ''}`}
        style={handleStyle}
        aria-label={tooltipLines.join('. ')}
      />
      <span
        className="catalog-flow-port-label"
        style={{ color: color ?? undefined }}
      >
        {portDisplayLabel(port)}
      </span>
      <span className="catalog-flow-port-tooltip" role="tooltip">
        {tooltipLines.map((line, lineIndex) => (
          <span
            key={`${port.id}-${lineIndex}`}
            className={lineIndex === 0 ? 'catalog-flow-port-tooltip-title' : undefined}
          >
            {line}
          </span>
        ))}
      </span>
    </div>
  )
}

export function BoundaryPorts({
  side,
  ports,
  portColors,
}: {
  side: 'TOP' | 'BOTTOM'
  ports: WorkflowPort[]
  portColors: Record<string, string>
}) {
  if (ports.length === 0) return null
  return (
    <div className={`catalog-flow-node-boundary catalog-flow-node-boundary-${side.toLowerCase()}`}>
      {ports.map((port, index) => (
        <PortRow
          key={`${side}-${port.id}`}
          port={port}
          side={side}
          index={index}
          count={ports.length}
          portColors={portColors}
        />
      ))}
    </div>
  )
}

export function InlineLateralPorts({
  leftPorts,
  rightPorts,
  portColors,
  dimMessagePorts,
}: {
  leftPorts: WorkflowPort[]
  rightPorts: WorkflowPort[]
  portColors: Record<string, string>
  dimMessagePorts: boolean
}) {
  if (leftPorts.length === 0 && rightPorts.length === 0) return null
  return (
    <div className="catalog-flow-node-ports">
      {leftPorts.length > 0 ? (
        <div className="catalog-flow-node-ports-column catalog-flow-node-ports-column-input">
          {leftPorts.map((port, index) => (
            <PortRow
              key={`left-${port.id}`}
              port={port}
              side="LEFT"
              index={index}
              count={leftPorts.length}
              portColors={portColors}
              inline
              dimmed={dimMessagePorts && isMessagePortId(port.id)}
            />
          ))}
        </div>
      ) : null}
      {rightPorts.length > 0 ? (
        <div className="catalog-flow-node-ports-column catalog-flow-node-ports-column-output">
          {rightPorts.map((port, index) => (
            <PortRow
              key={`right-${port.id}`}
              port={port}
              side="RIGHT"
              index={index}
              count={rightPorts.length}
              portColors={portColors}
              inline
              dimmed={dimMessagePorts && isMessagePortId(port.id)}
            />
          ))}
        </div>
      ) : null}
    </div>
  )
}
