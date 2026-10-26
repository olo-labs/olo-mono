/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useState } from 'react'

import { workflowConfigurationStore } from '../../store/workflowConfigurationStore'

import {

  removeVariable,

  uniqueVariableName,

  upsertVariable,

  workflowVariables,

} from '../../lib/workflowResources'

import {

  VARIABLE_SCOPE_HINTS,

  VARIABLE_SCOPE_LABELS,

  VARIABLE_SCOPES,

  normalizeVariableScope,

} from '../../lib/workflowVariables'

import type { WorkflowVariable } from '../../types/workflow'



const VARIABLE_TYPES = ['string', 'number', 'integer', 'boolean', 'object', 'array']



function emptyVariable(existing: WorkflowVariable[]): WorkflowVariable {

  return {

    name: uniqueVariableName(existing),

    type: 'string',

    description: '',

    required: false,

    scope: 'LOCAL',

    metadata: {},

  }

}



export function VariablesSection() {

  const draft = workflowConfigurationStore((s) => s.draft)

  const updateDraft = workflowConfigurationStore((s) => s.updateDraft)

  const variables = draft ? workflowVariables(draft) : []

  const [editingName, setEditingName] = useState<string | null>(null)

  const [form, setForm] = useState<WorkflowVariable | null>(null)



  if (!draft) {

    return <p className="builder-empty">Open a workflow to edit variables.</p>

  }



  const startEdit = (variable: WorkflowVariable) => {

    setEditingName(variable.name)

    setForm({ ...variable, scope: normalizeVariableScope(variable.scope) })

  }



  const startAdd = () => {

    const next = emptyVariable(variables)

    setEditingName('__new__')

    setForm(next)

  }



  const cancelEdit = () => {

    setEditingName(null)

    setForm(null)

  }



  const saveForm = () => {

    if (!form || !form.name.trim()) return

    const previous = editingName === '__new__' ? undefined : editingName ?? undefined

    updateDraft(

      upsertVariable(

        draft,

        { ...form, name: form.name.trim(), scope: normalizeVariableScope(form.scope) },

        previous,

      ),

    )

    cancelEdit()

  }



  const handleDelete = (name: string) => {

    updateDraft(removeVariable(draft, name))

    if (editingName === name) cancelEdit()

  }



  const formScope = normalizeVariableScope(form?.scope)



  return (

    <div className="builder-variables">

      <button type="button" className="builder-btn small" onClick={startAdd}>

        + Add variable

      </button>

      {variables.length === 0 && editingName !== '__new__' ? (

        <p className="builder-empty">No variables defined.</p>

      ) : (

        <ul className="builder-check-list">

          {variables.map((variable) => (

            <li key={variable.name} className="builder-variable-item">

              <button

                type="button"

                className="builder-variable-summary"

                onClick={() => startEdit(variable)}

              >

                <code>{variable.name}</code>

                <span className="builder-variable-type">

                  {variable.type ?? 'string'} · {VARIABLE_SCOPE_LABELS[normalizeVariableScope(variable.scope)]}

                </span>

              </button>

              <button

                type="button"

                className="builder-icon-btn danger"

                onClick={() => handleDelete(variable.name)}

                title="Remove variable"

              >

                ×

              </button>

            </li>

          ))}

        </ul>

      )}

      {form ? (

        <div className="builder-variable-form">

          <label className="builder-field">

            <span>Name</span>

            <input

              className="builder-input"

              value={form.name}

              onChange={(e) => setForm({ ...form, name: e.target.value })}

            />

          </label>

          <label className="builder-field">

            <span>Type</span>

            <select

              className="builder-input"

              value={form.type ?? 'string'}

              onChange={(e) => setForm({ ...form, type: e.target.value })}

            >

              {VARIABLE_TYPES.map((t) => (

                <option key={t} value={t}>{t}</option>

              ))}

            </select>

          </label>

          <label className="builder-field">

            <span>Scope</span>

            <select

              className="builder-input"

              value={formScope}

              onChange={(e) =>

                setForm({ ...form, scope: normalizeVariableScope(e.target.value) })

              }

            >

              {VARIABLE_SCOPES.map((scope) => (

                <option key={scope} value={scope}>

                  {VARIABLE_SCOPE_LABELS[scope]}

                </option>

              ))}

            </select>

          </label>

          <p className="builder-field-hint">{VARIABLE_SCOPE_HINTS[formScope]}</p>

          <label className="builder-field">

            <span>Description</span>

            <input

              className="builder-input"

              value={form.description ?? ''}

              onChange={(e) => setForm({ ...form, description: e.target.value })}

            />

          </label>

          <label className="builder-field checkbox">

            <input

              type="checkbox"

              checked={Boolean(form.required)}

              onChange={(e) => setForm({ ...form, required: e.target.checked })}

            />

            <span>Required</span>

          </label>

          <label className="builder-field">

            <span>Metadata role (optional)</span>

            <input

              className="builder-input"

              placeholder="e.g. return"

              value={String(form.metadata?.role ?? '')}

              onChange={(e) =>

                setForm({

                  ...form,

                  metadata: { ...form.metadata, role: e.target.value || undefined },

                })

              }

            />

          </label>

          <div className="builder-form-actions">

            <button type="button" className="builder-btn small" onClick={cancelEdit}>

              Cancel

            </button>

            <button type="button" className="builder-btn small primary" onClick={saveForm}>

              Save

            </button>

          </div>

        </div>

      ) : null}

    </div>

  )

}


