/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { memo } from 'react'

import {

  BaseEdge,

  EdgeLabelRenderer,

  getBezierPath,

  type EdgeProps,

} from '@xyflow/react'

import { DEFAULT_EDGE_STROKE, type CatalogFlowEdgeData } from '../../lib/canvasLabels'



export type { CatalogFlowEdgeData }



function CatalogFlowEdgeComponent({

  id,

  sourceX,

  sourceY,

  targetX,

  targetY,

  sourcePosition,

  targetPosition,

  data,

  selected,

  markerEnd,

  style,

}: EdgeProps) {

  const edgeData = (data ?? {}) as CatalogFlowEdgeData

  const strokeColor = edgeData.sourcePortColor ?? DEFAULT_EDGE_STROKE

  const [edgePath, labelX, labelY] = getBezierPath({

    sourceX,

    sourceY,

    targetX,

    targetY,

    sourcePosition,

    targetPosition,

  })



  return (

    <>

      <BaseEdge

        id={id}

        path={edgePath}

        markerEnd={markerEnd}

        style={{

          ...style,

          stroke: strokeColor,

          strokeWidth: selected ? 2 : 1.5,

        }}

      />

      {edgeData.tooltip ? (

        <EdgeLabelRenderer>

          <div

            className="catalog-flow-edge-label"

            style={{

              transform: `translate(-50%, -50%) translate(${labelX}px, ${labelY}px)`,

            }}

          >

            <span className="catalog-flow-edge-hit" aria-hidden />

            <span className="catalog-flow-edge-tooltip" role="tooltip">

              {edgeData.tooltip}

            </span>

          </div>

        </EdgeLabelRenderer>

      ) : null}

    </>

  )

}



export const CatalogFlowEdge = memo(CatalogFlowEdgeComponent)


