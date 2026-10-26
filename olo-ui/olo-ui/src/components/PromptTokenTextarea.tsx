/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
import { useMemo, useRef, useState, type SyntheticEvent } from 'react'
import {
  buildPromptInsertOptions,
  insertTextAtCursor,
  type PromptTokenInsertOption,
} from '../lib/promptTokens'

export interface PromptTokenTextareaProps {
  value: string
  onChange: (value: string) => void
  disabled?: boolean
  placeholder?: string
  rows?: number
  compact?: boolean
  workflowVariableNames?: string[]
  insertOptions?: PromptTokenInsertOption[]
  className?: string
  textareaClassName?: string
}

export function PromptTokenTextarea({
  value,
  onChange,
  disabled,
  placeholder,
  rows = 4,
  compact = false,
  workflowVariableNames = [],
  insertOptions,
  className,
  textareaClassName,
}: PromptTokenTextareaProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const [menuOpen, setMenuOpen] = useState(false)

  const options = useMemo(
    () => insertOptions ?? buildPromptInsertOptions(workflowVariableNames),
    [insertOptions, workflowVariableNames],
  )

  const quickRuntime = useMemo(
    () => options.filter((option) => option.group === 'Runtime' && ['message', 'tools', 'agents'].includes(option.label)),
    [options],
  )

  const variableOptions = useMemo(
    () => options.filter((option) => option.group === 'Variables'),
    [options],
  )

  const insertToken = (token: string) => {
    insertTextAtCursor(textareaRef.current, value, token, onChange)
    setMenuOpen(false)
  }

  const stopDrag = (event: SyntheticEvent) => {
    event.stopPropagation()
  }

  return (
    <div
      className={`prompt-token-textarea ${compact ? 'prompt-token-textarea-compact' : ''} ${className ?? ''}`.trim()}
      onPointerDown={compact ? stopDrag : undefined}
      onMouseDown={compact ? stopDrag : undefined}
    >
      <div className="prompt-token-toolbar">
        {quickRuntime.map((option) => (
          <button
            key={option.value}
            type="button"
            className="prompt-token-btn"
            disabled={disabled}
            title={option.description ?? option.value}
            onClick={() => insertToken(option.value)}
          >
            {`{${option.label}}`}
          </button>
        ))}
        <div className="prompt-token-toolbar-group">
          <button
            type="button"
            className="prompt-token-btn"
            disabled={disabled || variableOptions.length === 0}
            onClick={() => setMenuOpen((open) => !open)}
          >
            + variable
          </button>
          {menuOpen ? (
            <ul className="prompt-token-menu">
              {variableOptions.length === 0 ? (
                <li className="prompt-token-menu-empty">No workflow variables</li>
              ) : (
                variableOptions.map((option) => (
                  <li key={option.value}>
                    <button
                      type="button"
                      title={option.description}
                      onClick={() => insertToken(option.value)}
                    >
                      {option.label}
                    </button>
                  </li>
                ))
              )}
            </ul>
          ) : null}
        </div>
      </div>
      <textarea
        ref={textareaRef}
        className={`prompt-token-textarea-input ${textareaClassName ?? ''}`.trim()}
        rows={rows}
        value={value}
        disabled={disabled}
        placeholder={placeholder}
        onChange={(e) => onChange(e.target.value)}
        onPointerDown={compact ? stopDrag : undefined}
        onMouseDown={compact ? stopDrag : undefined}
      />
      {!compact ? (
        <p className="prompt-token-hint">
          Insert tokens like {'{message}'}, {'{tools}'}, {'{agents}'} — expanded at run time.
        </p>
      ) : null}
    </div>
  )
}
