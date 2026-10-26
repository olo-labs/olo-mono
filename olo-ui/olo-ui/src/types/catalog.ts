/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
/** Studio extension catalog (olo-core dist/catalog/catalog.json). */

export interface StudioCatalog {
  schemaVersion: string
  generatedAt?: string
  generatedBy?: string
  defaults?: CatalogDefaults
  catalogMetadata?: CatalogMetadata
  nodes?: CatalogNode[]
  tools?: CatalogTool[]
  hooks?: CatalogHook[]
  workflowPresets?: CatalogWorkflowPreset[]
  workflowTypes?: CatalogWorkflowType[]
  queues?: CatalogTemporalQueue[]
}

export interface CatalogWorkflowType {
  id: string
  label: string
  description?: string
  temporalMethod?: string
  workflowInterface?: string
}

export interface CatalogTemporalQueue {
  name: string
  label: string
  description?: string
  workflowType: string
}

export interface CatalogDefaults {
  connectionRules?: {
    primitives?: string[]
    wildcards?: string[]
    strategy?: string
    version?: string
  }
  connectionPolicy?: { maxInputs: number; maxOutputs: number }
  designer?: {
    nodeSize?: { width: number; height: number }
    resizable?: boolean
    draggable?: boolean
  }
  runtime?: { capabilities?: string[] }
}

export interface CatalogMetadata {
  parameterWidgets?: string[]
  portWireTypes?: string[]
}

export interface CatalogParameter {
  id: string
  label?: string
  type: string
  description?: string
  required?: boolean
  defaultValue?: unknown
  validation?: {
    minLength?: number
    maxLength?: number
    minimum?: number
    maximum?: number
    step?: number
  }
  values?: string[]
  visibleWhen?: Record<string, string>
  ui?: {
    widget?: string
    group?: string
    help?: string
    placeholder?: string
    order?: number
  }
}

export interface CatalogComponentBase {
  kind?: string
  id: string
  name?: string
  description?: string
  emoji?: string
  category?: string
  examples?: string[]
  designer?: {
    paletteGroup?: string
    searchKeywords?: string[]
    nodeSize?: { width: number; height: number }
  }
  parameters?: CatalogParameter[]
  inputs?: CatalogPort[]
  outputs?: CatalogPort[]
}

export interface CatalogPort {
  id: string
  label?: string
  name?: string
  schema?: string
  type?: string
  acceptType?: string | string[]
  direction?: string
  shortDescription?: string
  description?: string
  required?: boolean
  minConnections?: number
  maxConnections?: number | null
  ui?: {
    position?: string
    color?: string
  }
}

export type CatalogNode = CatalogComponentBase
export type CatalogTool = CatalogComponentBase
export type CatalogHook = CatalogComponentBase

export interface CatalogWorkflowPreset {
  id: string
  designer?: CatalogComponentBase['designer']
  parameters?: CatalogParameter[]
  inputs?: CatalogPort[]
  outputs?: CatalogPort[]
}
